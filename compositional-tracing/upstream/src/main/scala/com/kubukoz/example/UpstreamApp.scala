package com.kubukoz.example

import cats.effect.IOApp
import cats.effect.ExitCode
import cats.effect.IO
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.HttpRoutes
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import com.kubukoz.tracing.Span
import cats.implicits._

object UpstreamApp extends IOApp {
  import org.http4s.dsl.io._
  import org.http4s.implicits._

  //has to be done before logger is initialized
  System.setProperty("APP_NAME", "%yellow(upstream)")
  val logger = Slf4jLogger.getLogger[IO]

  val routes = HttpRoutes.of[IO] {
    case req @ POST -> Root / "execute" =>
      Span.fromHeaders(req.headers).flatMap { span =>
        logger.info(span.toMap)("Received execution request") *> Accepted()
      }
  }

  def run(args: List[String]): IO[ExitCode] =
    BlazeServerBuilder[IO]
      .bindHttp(8080, "0.0.0.0")
      .withHttpApp(routes.orNotFound)
      .resource
      .use(_ => logger.info("Started upstream") *> IO.never)
}
