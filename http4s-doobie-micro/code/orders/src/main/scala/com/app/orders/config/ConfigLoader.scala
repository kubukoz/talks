package com.app.orders.config
import cats.Applicative
import cats.effect.Sync
import cats.implicits._
import cats.mtl.DefaultApplicativeAsk
import com.typesafe.config.ConfigFactory

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
