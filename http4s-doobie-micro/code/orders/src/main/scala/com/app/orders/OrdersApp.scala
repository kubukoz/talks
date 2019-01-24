package com.app.orders

import cats.data.{EitherT, NonEmptyList}
import cats.effect._
import cats.implicits._
import io.circe.syntax._
import cats.temp.par._
import cats.{Applicative, ErrorControl}
import com.app.orders.OrderError.{SetNotDivisible, SushiKindNotFound}
import com.app.orders.config.{ConfigLoader, OrderServiceConfiguration}
import com.app.orders.payments.PaymentsClient
import com.app.orders.storage.OrderStorage
import com.app.orders.sushi.SushiClient
import com.app.payments.PaymentMade
import com.app.sushi.SushiKind
import eu.timepit.refined.types.numeric.{NonNegInt, PosInt, PosLong}
import io.circe.Encoder
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.impl.IntVar
import org.http4s.implicits._
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.chrisdavenport.log4cats.Logger
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.HttpRoutes
import fs2.Stream

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

object OrdersApp extends IOApp {
  implicit val configLoader: ConfigLoader[IO] = ConfigLoader.make

  override def run(args: List[String]): IO[ExitCode] =
    new OrdersServer[IO].run
}

class OrdersServer[F[_]: Timer: ContextShift: Par: ConfigLoader](implicit F: ConcurrentEffect[F]) {

  private val serverResource: Resource[F, Unit] =
    for {
      implicit0(config: OrderServiceConfiguration.Ask[F]) <- Resource.liftF(ConfigLoader[F].loadConfig)
      implicit0(client: Client[F])                        <- BlazeClientBuilder[F](ExecutionContext.global).resource
      module                                              <- Resource.liftF(OrdersModule.make[F])
      _                                                   <- BlazeServerBuilder[F].bindHttp(port = 2000).withHttpApp(module.routes.orNotFound).resource.void
      _                                                   <- Resource.liftF(module.streams.parTraverse(_.compile.drain))
    } yield ()

  val run: F[Nothing] = serverResource.use[Nothing](_ => F.never)
}

trait OrdersModule[F[_]] {
  def routes: HttpRoutes[F]
  def streams: List[Stream[F, Unit]]
}

object OrdersModule {

  def make[F[_]: Sync: Timer: Client: Par: OrderServiceConfiguration.Ask]: F[OrdersModule[F]] = {
    type E[A] = EitherT[F, NonEmptyList[OrderError], A]

    import com.olegpy.meow.hierarchy.deriveApplicativeAsk

    implicit val sushiClient: SushiClient[E]       = SushiClient.fromClient[F].mapK(EitherT.liftK)
    implicit val paymentsClient: PaymentsClient[E] = PaymentsClient.fromClient[F].mapK(EitherT.liftK)

    (
      OrderStorage.inMemory[F],
      Slf4jLogger.fromClass(classOf[OrdersModule[F]])
    ).mapN {
      case (implicit0(storage: OrderStorage[F]), implicit0(logger: Logger[F])) =>
        implicit val storageE: OrderStorage[E] = OrderStorage[F].mapK(EitherT.liftK)
        implicit val service                   = OrderService.make[E]

        new OrdersModule[F] {
          override val routes: HttpRoutes[F] = OrderRoutes.instance[E, F, NonEmptyList[OrderError]].routes
          override val streams: List[Stream[F, Unit]] = {
            val orderCountScheduledJob = Stream
              .repeatEval(storage.countOrders)
              .evalMap(count => Logger[F].info(show"Current order count: $count"))
              .zipLeft(Stream.sleep[F](5.seconds).repeat)

            List(orderCountScheduledJob)
          }
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
            case Right(result) => Ok(result.asJson)
            case Left(e)       => BadRequest(e.asJson)
          }
        case GET -> Root / "order" / UUIDVar(uuid) =>
          handler.control(OrderService[E].getStatus(OrderHandle(uuid))) {
            case Right(Some(result)) => Ok(result.asJson)
            case Right(None)         => NotFound()
            case Left(e)             => BadRequest(e.asJson)
          }
      }
    }
}

trait OrderService[F[_]] {
  def order(sushiKind: String, amount: PosInt): F[OrderedSushi]
  def getStatus(orderHandle: OrderHandle): F[Option[OrderStatus]]
}

object OrderService {
  def apply[F[_]](implicit F: OrderService[F]): OrderService[F] = F

  def make[F[_]: OrderStorage: SushiClient: OrderError.NelMonad: Par: PaymentsClient]: OrderService[F] =
    new OrderService[F] {

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

      def makePayment(price: PosLong): F[PaymentMade] = {
        PaymentsClient[F]
          .pay(price)
      }

      override def order(sushiKind: String, amount: PosInt): F[OrderedSushi] = {
        val orderSushi = SushiClient[F]
          .findKind(sushiKind)
          .flatMap(checkKind(sushiKind))
          .flatTap(checkSetSize(amount))
          .map(sushi => PosLong.unsafeFrom(amount.value * sushi.price.value))
          .flatMap(makePayment)
          .map(_.amount)

        (orderSushi, OrderStorage[F].saveOrder).mapN(OrderedSushi(amount, sushiKind, _, _))
      }

      override def getStatus(orderHandle: OrderHandle): F[Option[OrderStatus]] = OrderStorage[F].getStatus(orderHandle)
    }
}
