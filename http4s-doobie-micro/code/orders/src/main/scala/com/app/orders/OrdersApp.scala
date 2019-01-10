package com.app.orders

import cats.data.{EitherT, NonEmptyList}
import cats.effect._
import cats.implicits._
import cats.temp.par.{nonEmptyParToNonEmptyParallel, NonEmptyPar}
import cats.{~>, Applicative, ApplicativeError, ErrorControl, MonadError}
import com.app.orders.OrderError.{AmountNotPositive, PaymentFailed, SetNotDivisible, SushiKindNotFound}
import com.app.payments.PaymentMade
import com.app.sushi.SushiKind
import io.circe.{Decoder, Encoder}
import org.http4s.circe._
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.client.{Client, UnexpectedStatus}
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.{EntityDecoder, EntityEncoder, HttpRoutes, Method, Request, Status, Uri}

import scala.concurrent.ExecutionContext

object OrdersApp extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    new OrdersServer[IO].run
}

class OrdersServer[F[_]: Timer: ContextShift: NonEmptyPar](implicit F: ConcurrentEffect[F]) {
  private val serverResource: Resource[F, Unit] =
    for {
      implicit0(client: Client[F]) <- BlazeClientBuilder[F](ExecutionContext.global).resource
      _                            <- BlazeServerBuilder[F].bindHttp(port = 2000).withHttpApp(OrdersModule.make[F].routes.orNotFound).resource.void
    } yield ()

  val run: F[Nothing] = serverResource.use[Nothing](_ => F.never)
}

trait OrdersModule[F[_]] {
  def routes: HttpRoutes[F]
}

object OrdersModule {

  def make[F[_]: Sync: Client: NonEmptyPar]: OrdersModule[F] = {
    type E[A] = EitherT[F, NonEmptyList[OrderError], A]

    implicit val sushiClient: SushiClient[E]       = SushiClient.fromClient[F].mapK(EitherT.liftK)
    implicit val paymentsClient: PaymentsClient[F] = PaymentsClient.fromClient[F]

    implicit val orderService: OrderService[E] = OrderService.make[E, F]

    new OrdersModule[F] {
      override val routes: HttpRoutes[F] = OrderRoutes.instance[E, F, NonEmptyList[OrderError]].routes
    }
  }
}

trait OrderRoutes[F[_]] {
  def routes: HttpRoutes[F]
}

object OrderRoutes {
  implicit def entityEncoderForCirce[F[_]: Applicative, A: Encoder]: EntityEncoder[F, A] = jsonEncoderOf

  def instance[E[_]: OrderService: Sync, F[_]: Sync, Err: Encoder](
    implicit handler: ErrorControl[E, F, Err]): OrderRoutes[F] =
    new OrderRoutes[F] with Http4sDsl[F] {
      override val routes: HttpRoutes[F] = HttpRoutes.of[F] {
        case POST -> Root / "order" / sushiKind / IntVar(amount) =>
          handler.controlError {
            OrderService[E].order(sushiKind, amount).flatMap { totalPrice =>
              handler.accept(Ok(OrderedSushi(amount, sushiKind, totalPrice)))
            }
          }(BadRequest(_))
      }
    }
}

trait OrderService[F[_]] {
  def order(sushiKind: String, amount: Int): F[Long]
}

object OrderService {
  def apply[F[_]](implicit F: OrderService[F]): OrderService[F] = F

  def make[F[_]: OrderError.NelMonad: SushiClient: NonEmptyPar,
           G[_]: PaymentsClient: ApplicativeError[?[_], Throwable]](
    implicit handler: OrderError.NelErrorHandler[F, G]): OrderService[F] = new OrderService[F] {

    def checkKind(kind: String): Option[SushiKind] => F[SushiKind] =
      _.toRight(SushiKindNotFound(kind)).toEitherNel.liftTo[F]

    def checkSetSize(requested: Int)(kind: SushiKind): F[Unit] = {
      val rem = requested % kind.setSize

      if (rem == 0) Applicative[F].unit
      else {
        OrderError
          .NelMonad[F]
          .raiseError(SetNotDivisible(requested, kind.setSize, requested - rem).pure[NonEmptyList])
      }
    }

    def checkAmount(requested: Int): F[Unit] = {
      (requested > 0).guard[Option].liftTo[F](NonEmptyList.one(AmountNotPositive(requested)))
    }

    def makePayment(price: Long): F[PaymentMade] = handler.absolve {
      PaymentsClient[G]
        .pay(price)
        .map(_.asRight[NonEmptyList[OrderError]])
        .orElse(NonEmptyList.one[OrderError](PaymentFailed).asLeft[PaymentMade].pure[G])
    }

    override def order(sushiKind: String, amount: Int): F[Long] = {
      val placeOrder = SushiClient[F]
        .findKind(sushiKind)
        .flatMap(checkKind(sushiKind))
        .flatTap(checkSetSize(amount))
        .map(_.price * amount)
        .flatTap(makePayment)

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
        case resp if resp.status.isSuccess           => resp.as[SushiKind].map(_.some)
        case resp                                    => Sync[F].raiseError(UnexpectedStatus(resp.status))
      }
  }
}

trait PaymentsClient[F[_]] { self =>
  def pay(amount: Long): F[PaymentMade]

  def mapK[G[_]](f: F ~> G): PaymentsClient[G] = amount => f(self.pay(amount))
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

  type NelErrorHandler[F[_], G[_]] = ErrorControl[F, G, NonEmptyList[OrderError]]

  case class SushiKindNotFound(name: String)                               extends OrderError
  case class SetNotDivisible(requested: Int, setSize: Int, remainder: Int) extends OrderError
  case class AmountNotPositive(requested: Int)                             extends OrderError
  case object PaymentFailed                                                extends OrderError

  val message: OrderError => String = {
    case SushiKindNotFound(name)      => show"Couldn't find sushi kind: $name"
    case AmountNotPositive(requested) => show"The ordered amount must be positive, but it was $requested."
    case PaymentFailed                => show"Payment failed. Please try again later"
    case SetNotDivisible(requested, setSize, lower) =>
      val instead = if (lower > 0) show"$lower or ${lower + setSize}" else show"${lower + setSize}"
      show"The requested sushi kind is sold in sets of size $setSize (requested $requested, so maybe try ordering $instead instead?)"
  }

  implicit val encoder: Encoder[OrderError] = Encoder[String].contramap(message)
}
