package com.app.orders

import cats.data.{EitherT, NonEmptyList, OptionT}
import cats.effect._
import cats.implicits._
import cats.temp.par.{NonEmptyPar, nonEmptyParToNonEmptyParallel}
import cats.{Applicative, Monad, MonadError, ~>}
import com.app.orders.OrderError.{AmountNotPositive, SetNotDivisible, SushiKindNotFound}
import com.app.payments.PaymentMade
import com.app.sushi.SushiKind
import io.circe.{Decoder, Encoder}
import org.http4s.circe._
import org.http4s.client.{Client, UnexpectedStatus}
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.{EntityDecoder, EntityEncoder, HttpRoutes, Method, Request, Response, Status, Uri}
import org.omg.CosNaming.NamingContextPackage.NotFound

import scala.concurrent.ExecutionContext

object OrdersApp extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    new OrdersServer[IO].run
}

class OrdersServer[F[_]: Timer: ContextShift: NonEmptyPar](implicit F: ConcurrentEffect[F]) {
  private val serverResource: Resource[F, Unit] =
    for {
      client <- BlazeClientBuilder[F](ExecutionContext.global).resource
      _ <- {
        implicit val c: Client[F] = client

        BlazeServerBuilder[F].bindHttp(port = 2000).withHttpApp(OrdersModule.make[F].routes.orNotFound).resource.void
      }
    } yield ()

  val run: F[Nothing] = serverResource.use[Nothing](_ => F.never)
}

trait OrdersModule[F[_]] {
  def routes: HttpRoutes[F]
}

object OrdersModule {

  def make[F[_]: Sync: Client: NonEmptyPar]: OrdersModule[F] = {
    type E[A] = EitherT[F, NonEmptyList[OrderError], A]
    implicit val liftFToE: F ~> E = EitherT.liftK

    implicit val sushiClient: SushiClient[E]       = SushiClient.fromClient[F].mapK(EitherT.liftK)
    implicit val paymentsClient: PaymentsClient[E] = PaymentsClient.fromClient[F].mapK(EitherT.liftK)
    implicit val orderService: OrderService[E]     = OrderService.make[E]

    new OrdersModule[F] {
      override val routes: HttpRoutes[F] = OrderRoutes.instance[E, F].routes
    }
  }
}

trait OrderRoutes[F[_]] {
  def routes: HttpRoutes[F]
}

trait ErrorHandler[E[_], F[_]] {
  def handle(routes: PartialFunction[Request[F], E[Response[F]]]): HttpRoutes[F]
}

object ErrorHandler {
  implicit def handleEitherT[F[_]: Sync, E: Encoder]: ErrorHandler[EitherT[F, E, ?], F] = {

    new ErrorHandler[EitherT[F, E, ?], F] with Http4sDsl[F] {
      implicit def entityEncoderForCirce[A: Encoder]: EntityEncoder[F, A] = jsonEncoderOf

      override def handle(routes: PartialFunction[Request[F], EitherT[F, E, Response[F]]]): HttpRoutes[F] =
        HttpRoutes[F] { request =>
          OptionT {
            routes.lift.apply(request).traverse {
              _.value.flatMap {
                case Left(e)  => BadRequest(e)
                case Right(a) => a.pure[F]
              }
            }
          }
        }
    }
  }
}

object OrderRoutes {
  implicit def entityEncoderForCirce[F[_]: Applicative, A: Encoder]: EntityEncoder[F, A] = jsonEncoderOf

  def instance[E[_]: OrderService: Sync, F[_]: Monad](implicit handler: ErrorHandler[E, F],
                                                      liftToE: F ~> E): OrderRoutes[F] =
    new OrderRoutes[F] with Http4sDsl[F] {
      override val routes: HttpRoutes[F] = handler.handle {
        case POST -> Root / "order" / sushiKind / IntVar(amount) =>
          OrderService[E].order(sushiKind, amount).flatMap { totalPrice =>
            liftToE(Ok(OrderedSushi(amount, sushiKind, totalPrice)))
          }
      }
    }
}

trait OrderService[F[_]] {
  def order(sushiKind: String, amount: Int): F[Long]
}

object OrderService {
  def apply[F[_]](implicit F: OrderService[F]): OrderService[F] = F

  def make[F[_]: OrderError.NelMonad: SushiClient: PaymentsClient: NonEmptyPar]: OrderService[F] = new OrderService[F] {

    def checkKind(kind: String): Option[SushiKind] => F[SushiKind] =
      _.toRight(SushiKindNotFound(kind)).toEitherNel.liftTo[F]

    def checkSetSize(requested: Int)(kind: SushiKind): F[Unit] = {
      val rem = requested % kind.setSize

      if (rem == 0) Applicative[F].unit
      else {
        val err = SetNotDivisible(requested, kind.setSize, requested - rem)
        OrderError
          .NelMonad[F]
          .raiseError(err.pure[NonEmptyList])
      }
    }

    def checkAmount(requested: Int): F[Unit] = {
      (requested > 0).guard[Option].liftTo[F](NonEmptyList.one(AmountNotPositive(requested)))
    }

    override def order(sushiKind: String, amount: Int): F[Long] = {
      val placeOrder = SushiClient[F]
        .findKind(sushiKind)
        .flatMap(checkKind(sushiKind))
        .flatTap(checkSetSize(amount))
        .map(_.price * amount)
        .flatTap(PaymentsClient[F].pay)

      (checkAmount(amount), placeOrder).parMapN((_, totalPrice) => totalPrice)
    }
  }
}

trait SushiClient[F[_]] { self =>
  def findKind(name: String): F[Option[SushiKind]]

  def mapK[G[_]](f: F ~> G): SushiClient[G] = new SushiClient[G] {
    override def findKind(name: String): G[Option[SushiKind]] = f(self.findKind(name))
  }
}

object SushiClient {

  def apply[F[_]](implicit F: SushiClient[F]): SushiClient[F] = F

  def fromClient[F[_]: Sync](implicit client: Client[F]): SushiClient[F] = new SushiClient[F] {
    implicit def entityDecoderForCirce[A: Decoder]: EntityDecoder[F, A] = jsonOf

    override def findKind(name: String): F[Option[SushiKind]] =
      client.get(Uri.uri("http://localhost:3000/kinds/by-name/") / name) {
        case resp if resp.status === Status.NotFound => none[SushiKind].pure[F]
        case resp if resp.status.isSuccess           => resp.decodeJson[SushiKind].map(_.some)
        case resp                                    => Sync[F].raiseError(UnexpectedStatus(resp.status))
      }
  }
}

trait PaymentsClient[F[_]] { self =>
  def pay(amount: Long): F[PaymentMade]

  def mapK[G[_]](f: F ~> G): PaymentsClient[G] = new PaymentsClient[G] {
    override def pay(amount: Long): G[PaymentMade] = f(self.pay(amount))
  }
}

object PaymentsClient {

  def apply[F[_]](implicit F: PaymentsClient[F]): PaymentsClient[F] = F

  def fromClient[F[_]: Sync](implicit client: Client[F]): PaymentsClient[F] = new PaymentsClient[F] {
    implicit def entityDecoderForCirce[A: Decoder]: EntityDecoder[F, A] = jsonOf

    override def pay(amount: Long): F[PaymentMade] =
      client.expect(Request[F](Method.POST, Uri.uri("http://localhost:4000/pay") / amount.toString))
  }
}

sealed trait OrderError extends Product with Serializable

object OrderError {
  type NelMonad[F[_]] = MonadError[F, NonEmptyList[OrderError]]

  def NelMonad[F[_]](implicit F: NelMonad[F]): NelMonad[F] = F

  case class SushiKindNotFound(name: String)                               extends OrderError
  case class SetNotDivisible(requested: Int, setSize: Int, remainder: Int) extends OrderError
  case class AmountNotPositive(requested: Int)                             extends OrderError

  val message: OrderError => String = {
    case SushiKindNotFound(name)      => show"Couldn't find sushi kind: $name"
    case AmountNotPositive(requested) => show"The ordered amount must be positive, but it was $requested."
    case OrderError.SetNotDivisible(requested, setSize, lower) =>
      val instead = if (lower > 0) show"$lower or ${lower + setSize}" else show"${lower + setSize}"
      show"The requested sushi kind is sold in sets of size $setSize (requested $requested, so maybe try ordering $instead instead?)"
  }

  implicit val encoder: Encoder[OrderError] = Encoder[String].contramap(message)
}
