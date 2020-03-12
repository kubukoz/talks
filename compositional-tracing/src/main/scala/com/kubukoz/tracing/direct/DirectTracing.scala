package com.kubukoz.tracing.direct

import cats.effect.IOApp

import cats.effect.ExitCode
import cats.effect.IO
import java.{util => ju}
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import cats.tagless.finalAlg
import cats.implicits._
import cats.effect.ContextShift
import cats.Show
import com.kubukoz.tracing.Span
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import cats.effect.Timer
import com.kubukoz.tracing.TraceReporter
import cats.effect.Blocker
import zipkin2.Endpoint

object DirectTracing extends IOApp {

  //has to be done before logger is initialized
  System.setProperty("APP_NAME", "%magenta(client  )")

  def run(args: List[String]): IO[ExitCode] =
    Blocker[IO]
      .flatMap(
        TraceReporter.zipkin[IO](Endpoint.newBuilder().serviceName("client").build(), _)
      )
      .use { implicit tracer =>
        BlazeClientBuilder[IO](ExecutionContext.global).resource.use { implicit client =>
          val bl = BusinessLogic.instance

          def exec(msg: String) =
            Span.create[IO](msg).flatMap { span =>
              tracer.trace(span) {
                IO(ju.UUID.randomUUID()).map(Args(_, msg)).flatMap(bl.execute(_, span))
              }
            }

          exec("hello") // &> exec("bye")
        } *> tracer.flush *> IO.never
      }
      .as(ExitCode.Success)
}

final case class Result(message: String)
final case class Args(requestId: ju.UUID, message: String)

object Args {
  implicit val show: Show[Args] = Show.fromToString
}

@finalAlg
trait BusinessLogic[F[_]] {
  def execute(args: Args, span: Span): F[Result]
}

object BusinessLogic {

  def instance(
    implicit cs: ContextShift[IO],
    timer: Timer[IO],
    client: Client[IO],
    tracer: TraceReporter[IO]
  ): BusinessLogic[IO] = {
    val logger = Slf4jLogger.getLogger[IO]

    import org.http4s.client.dsl.io._
    import org.http4s.Method._
    import org.http4s.implicits._

    def newSpan[A](
      name: String,
      parent: Span,
      extraTags: Map[String, String] = Map.empty
    )(
      fa: Span => IO[A]
    ): IO[A] =
      Span
        .create[IO](name, parent.some)
        .map(_.withValues(_ ++ extraTags))
        .flatMap(newSpan => tracer.trace(newSpan)(fa(newSpan)))

    new BusinessLogic[IO] {
      private def databaseCall(rootSpan: Span) =
        newSpan(
          "db-call",
          rootSpan,
          Map("db.query" -> "select * from users where id = ?")
        ) { span =>
          logger.info(span.toMap)("Running db call") *>
            IO.sleep(100.millis)
        }

      private def clientCall(rootSpan: Span) =
        newSpan("remote-call", rootSpan) { child =>
          client.successful(
            POST(
              uri"http://localhost:8080/execute",
              child.toTraceHeaders.toList: _*
            )
          )
        }

      def execute(args: Args, span: Span): IO[Result] =
        for {
          _ <- logger.info(span.toMap)(show"Executing request $args")
          _ <- databaseCall(span)
          _ <- clientCall(span)
          _ <- logger.info(span.toMap)(show"Executed request $args")
        } yield Result(show"${args.message} finished")
    }
  }
}
