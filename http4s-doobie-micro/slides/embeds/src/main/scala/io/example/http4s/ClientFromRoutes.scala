package io.example.http4s

import cats.effect.Sync
import io.circe.Decoder
import org.http4s.HttpRoutes
import org.http4s.client.Client
import org.http4s.dsl.Http4sDsl
import io.circe.syntax._
import org.http4s.circe._
import org.http4s.implicits._
import cats.implicits._
case class PaymentRequest(amount: Long)

object PaymentRequest {
  implicit val decoder: Decoder[PaymentRequest] = Decoder.failedWithMessage("stub")
}

object PaypalClient {

  def make[F[_]: Sync]: Client[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._

    Client.fromHttpApp {
      HttpRoutes
        .of[F] {
          case req @ POST -> Root / "make-payment" =>
            for {
              body     <- req.decodeJson[PaymentRequest]
              response <- Ok(s"Accepted payment of ${body.amount}".asJson)
            } yield response
        }
        .orNotFound
    }
  }
}
