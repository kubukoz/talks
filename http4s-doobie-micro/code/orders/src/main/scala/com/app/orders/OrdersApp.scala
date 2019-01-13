package com.app.orders

import cats.data.{EitherT, NonEmptyList}
import cats.effect._
import cats.implicits._
import io.circe.syntax._
import cats.temp.par.NonEmptyPar
import cats.{Applicative, ErrorControl, MonadError}
import com.app.orders.OrderError.{PaymentFailed, SetNotDivisible, SushiKindNotFound}
import com.app.orders.config.{ConfigLoader, OrderServiceConfiguration}
import com.app.orders.payments.PaymentsClient
import com.app.orders.sushi.SushiClient
import com.app.payments.PaymentMade
import com.app.sushi.SushiKind
import eu.timepit.refined.types.numeric.{NonNegInt, PosInt, PosLong}
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.circe.Encoder
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.impl.IntVar
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.HttpRoutes

import scala.concurrent.ExecutionContext

object OrdersApp extends IOApp {
  implicit val configLoader: ConfigLoader[IO] = ConfigLoader.make

  override def run(args: List[String]): IO[ExitCode] =
    new OrdersServer[IO].run
}

class OrdersServer[F[_]: Timer: ContextShift: NonEmptyPar: ConfigLoader](implicit F: ConcurrentEffect[F]) {

  private val serverResource: Resource[F, Unit] =
    for {
      implicit0(config: OrderServiceConfiguration.Ask[F]) <- Resource.liftF(ConfigLoader[F].loadConfig)
      implicit0(client: Client[F])                        <- BlazeClientBuilder[F](ExecutionContext.global).resource
      module                                              <- Resource.liftF(OrdersModule.make[F])
      _                                                   <- BlazeServerBuilder[F].bindHttp(port = 2000).withHttpApp(module.routes.orNotFound).resource.void
    } yield ()

  val run: F[Nothing] = serverResource.use[Nothing](_ => F.never)
}

trait OrdersModule[F[_]] {
  def routes: HttpRoutes[F]
}

object OrdersModule {

  def make[F[_]: Sync: Client: NonEmptyPar: OrderServiceConfiguration.Ask]: F[OrdersModule[F]] = {
    type E[A] = EitherT[F, NonEmptyList[OrderError], A]

    import com.olegpy.meow.hierarchy.deriveApplicativeAsk

    implicit val sushiClient: SushiClient[E]       = SushiClient.fromClient[F].mapK(EitherT.liftK)
    implicit val paymentsClient: PaymentsClient[F] = PaymentsClient.fromClient[F]

    Slf4jLogger
      .fromClass[F](classOf[OrderService[E]])
      .map(implicit l => OrderService.make[E, F])
      .map { implicit orderService =>
        new OrdersModule[F] {
          override val routes: HttpRoutes[F] = OrderRoutes.instance[E, F, NonEmptyList[OrderError]].routes
        }
      }
  }
}

trait OrderRoutes[F[_]] {
  def routes: HttpRoutes[F]
}

object OrderRoutes {
  object PosIntVar {

    def unapply(str: String): Option[PosInt] = IntVar.unapply(str).flatMap {
      PosInt.from(_).toOption
    }
  }

  def instance[E[_]: OrderService: Sync, F[_]: Sync, Err: Encoder](
    implicit handler: ErrorControl[E, F, Err]): OrderRoutes[F] =
    new OrderRoutes[F] with Http4sDsl[F] {
      override val routes: HttpRoutes[F] = HttpRoutes.of[F] {
        case POST -> Root / "order" / sushiKind / PosIntVar(amount) =>
          handler.control(OrderService[E].order(sushiKind, amount)) {
            case Right(totalPrice) => Ok(OrderedSushi(amount, sushiKind, totalPrice).asJson)
            case Left(e)           => BadRequest(e.asJson)
          }
      }
    }
}

trait OrderService[F[_]] {
  def order(sushiKind: String, amount: PosInt): F[PosLong]
}

object OrderService {
  def apply[F[_]](implicit F: OrderService[F]): OrderService[F] = F

  def make[F[_]: OrderError.NelMonad: SushiClient: NonEmptyPar,
           G[_]: PaymentsClient: MonadError[?[_], Throwable]: Logger](
    implicit handler: OrderError.NelErrorHandler[F, G]): OrderService[F] = new OrderService[F] {

    def checkKind(kind: String): Option[SushiKind] => F[SushiKind] =
      _.toRight(SushiKindNotFound(kind)).toEitherNel.liftTo[F]

    def checkSetSize(requested: PosInt)(kind: SushiKind): F[Unit] = {
      val rem = requested.value % kind.setSize.value

      if (rem == 0) Applicative[F].unit
      else {
        OrderError
          .NelMonad[F]
          .raiseError(
            SetNotDivisible(requested, kind.setSize, NonNegInt.unsafeFrom(requested.value - rem)).pure[NonEmptyList])
      }
    }

    def makePayment(price: PosLong): F[PaymentMade] = handler.absolve {
      PaymentsClient[G]
        .pay(price)
        .attempt
        .flatMap {
          _.bitraverse(Logger[G]
                         .error(_)("Unexpected error while calling Payment service")
                         .as((PaymentFailed: OrderError).pure[NonEmptyList]),
                       _.pure[G])
        }
    }

    override def order(sushiKind: String, amount: PosInt): F[PosLong] = {
      SushiClient[F]
        .findKind(sushiKind)
        .flatMap(checkKind(sushiKind))
        .flatTap(checkSetSize(amount))
        .map(sushi => PosLong.unsafeFrom(amount.value * sushi.price.value))
        .flatTap(makePayment)
    }
  }
}

sealed trait OrderError extends Product with Serializable

object OrderError {
  type NelMonad[F[_]] = MonadError[F, NonEmptyList[OrderError]]
  def NelMonad[F[_]](implicit F: NelMonad[F]): NelMonad[F] = F

  type NelErrorHandler[F[_], G[_]] = ErrorControl[F, G, NonEmptyList[OrderError]]

  case class SushiKindNotFound(name: String)                                       extends OrderError
  case class SetNotDivisible(requested: PosInt, setSize: PosInt, lower: NonNegInt) extends OrderError
  case object PaymentFailed                                                        extends OrderError

  val message: OrderError => String = {
    case SushiKindNotFound(name) => show"Couldn't find sushi kind: $name"
    case PaymentFailed           => show"Payment failed. Please try again later"
    case SetNotDivisible(requested, setSize, lower) =>
      val instead =
        if (lower.value > 0) show"${lower.value} or ${lower.value + setSize.value}"
        else show"${lower.value + setSize.value}"
      show"The requested sushi kind is sold in sets of size ${setSize.value} (requested ${requested.value}, so maybe try ordering $instead instead?)"
  }

  implicit val encoder: Encoder[OrderError] = Encoder[String].contramap(message)
}
