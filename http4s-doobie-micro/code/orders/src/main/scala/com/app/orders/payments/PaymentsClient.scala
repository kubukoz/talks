package com.app.orders.payments

import cats.effect.Sync
import cats.implicits._
import cats.~>
import com.app.orders.util.AskFunctions
import com.app.payments.{PaymentMade, PaymentRequest}
import eu.timepit.refined.types.numeric.PosLong
import io.circe.syntax._
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.{Method, Request, Uri}
import pureconfig.ConfigReader
import scalaz.deriving

trait PaymentsClient[F[_]] { self =>
  def pay(amount: PosLong): F[PaymentMade]

  def mapK[G[_]](f: F ~> G): PaymentsClient[G] = amount => f(pay(amount))
}

object PaymentsClient {
  import com.app.orders.http4s.UriInstances._

  @deriving(ConfigReader)
  case class Configuration(baseUrl: Uri)

  object Configuration extends AskFunctions[Configuration]

  def apply[F[_]](implicit F: PaymentsClient[F]): PaymentsClient[F] = F

  def fromClient[F[_]: Sync: Configuration.Ask](implicit client: Client[F]): PaymentsClient[F] = new PaymentsClient[F] {
    override def pay(amount: PosLong): F[PaymentMade] = Configuration.Ask[F].ask.flatMap { config =>
      client.expect(Request[F](Method.POST, config.baseUrl / "pay").withEntity(PaymentRequest(amount).asJson))(
        jsonOf[F, PaymentMade])
    }
  }
}
