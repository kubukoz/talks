package com.app.payments

import cats.effect._
import cats.implicits._
import cats.{Applicative, Functor}
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.circe.syntax._
import io.circe.Encoder
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.{EntityEncoder, HttpRoutes, Method, Request, Uri}

object PaymentsApp extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    new PaymentsServer[IO].run
}

class PaymentsServer[F[_]: Timer: ContextShift](implicit F: ConcurrentEffect[F]) {
  private val serverResource: Resource[F, Unit] = Resource.liftF(PaymentsModule.make[F]).flatMap { module =>
    BlazeServerBuilder[F].bindHttp(port = 4000).withHttpApp(module.routes.orNotFound).resource.void
  }

  val run: F[Nothing] = serverResource.use[Nothing](_ => F.never)
}

trait PaymentsModule[F[_]] {
  def routes: HttpRoutes[F]
}

object PaymentsModule {

  def make[F[_]: Sync]: F[PaymentsModule[F]] = {
    implicit val mockPaypalClient: Client[F] = {
      val dsl = new Http4sDsl[F] {}
      import dsl._

      Client.fromHttpApp {
        HttpRoutes
          .of[F] {
            case req @ POST -> Root / "make-payment" =>
              for {
                amount   <- req.params.get("amount").liftTo[F](new Throwable("'amount' parameter missing!"))
                response <- Ok(s"Accepted payment of $amount".asJson)
              } yield response
          }
          .orNotFound
      }
    }

    implicit val paymentService: PaymentService[F] = PaymentService.paypalPaymentService

    Slf4jLogger.fromClass(classOf[PaymentRoutes[F]]).map { implicit logger =>
      new PaymentsModule[F] {
        override val routes: HttpRoutes[F] = PaymentRoutes.instance[F].routes
      }
    }
  }
}

trait PaymentRoutes[F[_]] {
  def routes: HttpRoutes[F]
}

object PaymentRoutes {
  implicit def entityEncoderForCirce[F[_]: Applicative, A: Encoder]: EntityEncoder[F, A] = jsonEncoderOf

  def instance[F[_]: Sync: PaymentService: Logger]: PaymentRoutes[F] = new PaymentRoutes[F] with Http4sDsl[F] {
    override val routes: HttpRoutes[F] = HttpRoutes.of {
      case POST -> Root / "pay" / LongVar(amount) =>
        Logger[F].info(show"Trying to pay $amount") *>
          PaymentService[F].pay(amount) *>
          Logger[F].info(show"Paid $amount") *>
          Ok(PaymentMade(amount))
    }
  }
}

trait PaymentService[F[_]] {
  def pay(amount: Long): F[Unit]
}

object PaymentService {
  def apply[F[_]](implicit F: PaymentService[F]): PaymentService[F] = F

  def paypalPaymentService[F[_]: Functor](implicit client: Client[F]): PaymentService[F] =
    (amount: Long) =>
      client.successful(Request[F](Method.POST, uri = Uri.unsafeFromString(s"/make-payment?amount=$amount"))).void
}
