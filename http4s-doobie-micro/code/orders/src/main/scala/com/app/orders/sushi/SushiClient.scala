package com.app.orders.sushi

import cats.effect.Sync
import cats.implicits.none
import cats.~>
import com.app.orders.util.AskFunctions
import com.app.sushi.SushiKind
import io.circe.Decoder
import org.http4s.circe.jsonOf
import org.http4s.client.{Client, UnexpectedStatus}
import org.http4s.{EntityDecoder, Status, Uri}
import pureconfig.ConfigReader
import scalaz.deriving
import cats.implicits._

object SushiClient {
  import com.app.orders.http4s.UriInstances._

  @deriving(ConfigReader)
  case class Configuration(baseUrl: Uri)

  object Configuration extends AskFunctions[Configuration]

  def apply[F[_]](implicit F: SushiClient[F]): SushiClient[F] = F

  def fromClient[F[_]: Sync: Configuration.Ask](implicit client: Client[F]): SushiClient[F] = new SushiClient[F] {
    implicit def entityDecoderForCirce[A: Decoder]: EntityDecoder[F, A] = jsonOf

    override def findKind(name: String): F[Option[SushiKind]] = Configuration.Ask[F].ask.flatMap { config =>
      client.get(config.baseUrl / "kinds" / "by-name" / name) {
        case resp if resp.status === Status.NotFound => none[SushiKind].pure[F]
        case resp if resp.status.isSuccess           => resp.as[SushiKind].map(_.some)
        case resp                                    => Sync[F].raiseError(UnexpectedStatus(resp.status))
      }
    }
  }
}

trait SushiClient[F[_]] { self =>
  def findKind(name: String): F[Option[SushiKind]]

  def mapK[G[_]](f: F ~> G): SushiClient[G] = name => f(self.findKind(name))
}
