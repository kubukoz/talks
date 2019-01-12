package com.app.orders.payments

import cats.effect.Sync
import cats.~>
import com.app.orders.util.AskFunctions
import com.app.payments.PaymentMade
import io.circe.Decoder
import org.http4s.{EntityDecoder, Method, Request, Uri}
import org.http4s.client.Client
import pureconfig.ConfigReader
import scalaz.deriving
import cats.implicits._
import org.http4s.circe._

trait PaymentsClient[F[_]] { self =>
  def pay(amount: Long): F[PaymentMade]

  def mapK[G[_]](f: F ~> G): PaymentsClient[G] = amount => f(self.pay(amount))
}
object PaymentsClient {
  import com.app.orders.http4s.UriInstances._

  @deriving(ConfigReader)
  case class Configuration(baseUrl: Uri)

  object Configuration extends AskFunctions[Configuration]

  def apply[F[_]](implicit F: PaymentsClient[F]): PaymentsClient[F] = F

  def fromClient[F[_]: Sync: Configuration.Ask](implicit client: Client[F]): PaymentsClient[F] = new PaymentsClient[F] {
    implicit def entityDecoderForCirce[A: Decoder]: EntityDecoder[F, A] = jsonOf

    override def pay(amount: Long): F[PaymentMade] = Configuration.Ask[F].ask.flatMap { config =>
      client.expect(Request[F](Method.POST, config.baseUrl / "pay" / amount.toString))
    }
  }
}