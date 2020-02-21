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

object DirectTracing extends IOApp {

  //has to be done before logger is initialized
  System.setProperty("APP_NAME", "%magenta(client  )")

  def run(args: List[String]): IO[ExitCode] =
    BlazeClientBuilder[IO](ExecutionContext.global)
      .resource
      .use { implicit client =>
        val bl = BusinessLogic.instance

        def exec(msg: String) =
          (IO(ju.UUID.randomUUID()).map(Args(_, msg)), Span.create(msg))
            .mapN(bl.execute)
            .flatten

        exec("hello") &> exec("bye")
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
    client: Client[IO]
  ): BusinessLogic[IO] = {
    val logger = Slf4jLogger.getLogger[IO]

    import org.http4s.client.dsl.io._
    import org.http4s.Method._
    import org.http4s.implicits._

    new BusinessLogic[IO] {
      def execute(args: Args, span: Span): IO[Result] =
        for {
          _ <- logger.info(span.toMap)(show"Executing request $args")
          _ <- IO.sleep(100.millis)
          _ <- client.successful(
                POST(uri"http://localhost:8080/execute", span.toTraceHeaders.toList: _*)
              )
          _ <- logger.info(span.toMap)(show"Executed request $args")
        } yield Result(show"${args.message} finished")
    }
  }
}
