package com.app.orders

import cats.data.{EitherT, NonEmptyList}
import cats.effect._
import cats.implicits._
import cats.mtl.DefaultApplicativeAsk
import cats.temp.par.{nonEmptyParToNonEmptyParallel, NonEmptyPar}
import cats.{Applicative, ErrorControl, MonadError}
import com.app.orders.OrderError.{AmountNotPositive, PaymentFailed, SetNotDivisible, SushiKindNotFound}
import com.app.orders.config.OrderServiceConfiguration
import com.app.orders.payments.PaymentsClient
import com.app.orders.sushi.SushiClient
import com.app.payments.PaymentMade
import com.app.sushi.SushiKind
import com.typesafe.config.ConfigFactory
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.circe.Encoder
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.{EntityEncoder, HttpRoutes}

import scala.concurrent.ExecutionContext

object OrdersApp extends IOApp {
  implicit val configLoader: ConfigLoader[IO] = ConfigLoader.make

  override def run(args: List[String]): IO[ExitCode] =
    new OrdersServer[IO].run
}

trait ConfigLoader[F[_]] {
  def loadConfig: F[OrderServiceConfiguration.Ask[F]]
}

object ConfigLoader {
  def apply[F[_]](implicit F: ConfigLoader[F]): ConfigLoader[F] = F

  def make[F[_]: Sync]: ConfigLoader[F] = new ConfigLoader[F] {
    override val loadConfig: F[OrderServiceConfiguration.Ask[F]] = {
      Sync[F]
        .delay(ConfigFactory.load())
        .flatMap { raw =>
          Sync[F].catchNonFatal(pureconfig.loadConfigOrThrow[OrderServiceConfiguration](raw))
        }
        .map { config =>
          new DefaultApplicativeAsk[F, OrderServiceConfiguration] {
            override val applicative: Applicative[F]       = Applicative[F]
            override val ask: F[OrderServiceConfiguration] = config.pure[F]
          }
        }
    }
  }
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
           G[_]: PaymentsClient: MonadError[?[_], Throwable]: Logger](
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
        .attempt
        .flatMap {
          _.bitraverse(Logger[G]
                         .error(_)("Unexpected error while calling Payment service")
                         .as((PaymentFailed: OrderError).pure[NonEmptyList]),
                       _.pure[G])
        }
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
