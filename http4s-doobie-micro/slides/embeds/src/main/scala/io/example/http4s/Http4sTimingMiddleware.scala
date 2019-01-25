package io.example.http4s

import cats.FlatMap
import cats.data.Kleisli
import cats.effect.Clock
import cats.implicits._
import org.http4s.util.CaseInsensitiveString
import org.http4s.{Header, HttpApp}

import scala.concurrent.duration.TimeUnit

object ResponseTiming {
  def apply[F[_]: Clock: FlatMap](
    http: HttpApp[F],
    timeUnit: TimeUnit,
    headerName: CaseInsensitiveString): HttpApp[F] = {

    Kleisli { req =>
      for {
        before <- Clock[F].monotonic(timeUnit)
        resp   <- http(req)
        after  <- Clock[F].monotonic(timeUnit)
        header = Header(headerName.value, s"${after - before}")
      } yield resp.putHeaders(header)
    }
  }
}
