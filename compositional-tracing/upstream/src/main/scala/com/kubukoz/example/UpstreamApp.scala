package com.kubukoz.example

import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.HttpRoutes
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import com.kubukoz.tracing.Span
import cats.implicits._
import com.kubukoz.tracing.TraceReporter
import scala.concurrent.duration._
import cats.effect.Blocker
import zipkin2.Endpoint

object UpstreamApp extends IOApp {
  import org.http4s.dsl.io._
  import org.http4s.implicits._

  //has to be done before logger is initialized
  System.setProperty("APP_NAME", "%yellow(upstream)")
  val logger = Slf4jLogger.getLogger[IO]

  def routes(reporter: TraceReporter[IO]) = HttpRoutes.of[IO] {
    case req @ POST -> (path @ Root / "execute") =>
      Span.fromHeaders[IO](path.asString)(req.headers).flatMap { span =>
        reporter.trace(span) {
          logger.info(span.toMap)("Received execution request") *>
            Span.create[IO]("impl", span.some).flatMap { span =>
              reporter.trace(span) {
                IO.sleep(100.millis) *>
                  logger.info(span.toMap)("Completed execution") *>
                  Accepted()
              }
            }
        }
      }
  }

  def run(args: List[String]): IO[ExitCode] = {
    Blocker[IO]
      .flatMap(
        TraceReporter.zipkin[IO](
          Endpoint.newBuilder().ip("0.0.0.0").port(8080).serviceName("upstream").build(),
          _
        )
      )
      .flatMap { tracer =>
        BlazeServerBuilder[IO]
          .bindHttp(8080, "0.0.0.0")
          .withHttpApp(routes(tracer).orNotFound)
          .resource
      }
  }.use(_ => logger.info("Started upstream") *> IO.never)
}
