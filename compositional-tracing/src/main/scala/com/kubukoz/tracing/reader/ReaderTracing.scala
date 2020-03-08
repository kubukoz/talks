package com.kubukoz.tracing.reader

import cats.effect.IOApp

import cats.effect.ExitCode
import cats.effect.IO
import java.{util => ju}
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import cats.tagless.finalAlg
import cats.implicits._
import cats.effect.ContextShift
import cats.Show
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import cats.effect.Timer
import cats.effect.Blocker
import natchez.Trace
import cats.effect.Sync
import cats.data.Kleisli
import natchez.Span
import org.http4s.Header
import io.chrisdavenport.log4cats.MessageLogger
import natchez.Kernel
import cats.effect.Resource
import org.http4s.Response
import natchez.EntryPoint
import com.kubukoz.tracing.TraceReporter
import zipkin2.reporter.AsyncReporter
import zipkin2.reporter.okhttp3.OkHttpSender
import java.time.Instant
import zipkin2.Endpoint
import natchez.TraceValue
import cats.effect.concurrent.Ref
import cats.Applicative
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.StructuredLogger
import cats.FlatMap
import cats.effect.Bracket
import cats.Defer
import cats.~>
import cats.effect.SyncIO
import cats.effect.SyncEffect
import com.kubukoz.tracing.logging.MDCLogging
import natchez.log.Log

object Zipkin {

  def entryPoint[F[_]: Sync: ContextShift](
    appName: String,
    blocker: Blocker
  ): Resource[F, EntryPoint[F]] =
    Resource
      .fromAutoCloseable(
        Sync[F].delay(
          AsyncReporter.create(OkHttpSender.create("http://localhost:9411/api/v2/spans"))
        )
      )
      .map { reporter =>
        val now = Sync[F].delay(Instant.now())
        val endpoint: Endpoint =
          Endpoint.newBuilder().ip("0.0.0.0").port(9411).serviceName(appName).build()

        new EntryPoint[F] {

          private def make(span: com.kubukoz.tracing.Span): Resource[F, Span[F]] =
            Resource.suspend {
              (Ref[F].of(span), now).tupled.map {
                case (spanRef, started) =>
                  val lifecycle = Resource.make(Applicative[F].unit) { _ =>
                    (spanRef.get, now)
                      .mapN(TraceReporter.convertSpan(endpoint, _, started, _))
                      .flatMap(span =>
                        Sync[F].delay(reporter.report(span)) *> blocker
                          .delay[F, Unit](reporter.flush())
                      )
                  }

                  val theSpan: Span[F] = new Span[F] {
                    def put(fields: (String, TraceValue)*): F[Unit] =
                      spanRef.update(_.withValues(_ ++ fields))

                    val kernel: F[Kernel] = spanRef.get.map(span => Kernel(span.toMap))

                    def span(name: String): Resource[F, Span[F]] =
                      Resource.suspend {
                        spanRef
                          .get
                          .flatMap { parent =>
                            com.kubukoz.tracing.Span.create(name, parent.some)
                          }
                          .map(make)
                      }

                  }

                  lifecycle.as(theSpan)
              }
            }

          def root(name: String): Resource[F, Span[F]] =
            Resource.suspend(com.kubukoz.tracing.Span.create[F](name, None).map(make))

          //Thankfully we don't use these in the examples, but there's an open PR to natchez
          //to add opentracing support - which will work with zipkin with little extra effort.
          def continue(name: String, kernel: Kernel): Resource[F, Span[F]] =
            Resource.liftF(new Throwable("not implemented").raiseError[F, Span[F]])

          def continueOrElseRoot(name: String, kernel: Kernel): Resource[F, Span[F]] =
            Resource.liftF(new Throwable("not implemented").raiseError[F, Span[F]])

        }
      }
}

object ReaderTracing extends IOApp {

  //has to be done before logger is initialized
  System.setProperty("APP_NAME", "%magenta(client  )")

  type Traced[A] = Kleisli[IO, Span[IO], A]

  implicit val rawLogger = Slf4jLogger.getLogger[IO]

  def loggerBy[F[_]: FlatMap](
    underlying: StructuredLogger[F]
  )(
    ctx: F[Map[String, String]]
  ): MessageLogger[F] = {
    def withCtx(
      withUnderlying: (StructuredLogger[F], Map[String, String]) => F[Unit]
    ): F[Unit] = ctx.flatMap(withUnderlying(underlying, _))

    new Logger[F] {
      def error(message: => String): F[Unit] = withCtx(_.error(_)(message))
      def warn(message: => String): F[Unit] = withCtx(_.warn(_)(message))
      def info(message: => String): F[Unit] = withCtx(_.info(_)(message))
      def debug(message: => String): F[Unit] = withCtx(_.debug(_)(message))
      def trace(message: => String): F[Unit] = withCtx(_.trace(_)(message))

      def error(t: Throwable)(message: => String): F[Unit] =
        withCtx(_.error(_, t)(message))

      def warn(t: Throwable)(message: => String): F[Unit] =
        withCtx(_.warn(_, t)(message))

      def info(t: Throwable)(message: => String): F[Unit] =
        withCtx(_.info(_, t)(message))

      def debug(t: Throwable)(message: => String): F[Unit] =
        withCtx(_.debug(_, t)(message))

      def trace(t: Throwable)(message: => String): F[Unit] =
        withCtx(_.trace(_, t)(message))
    }
  }

  //Runs the given code block with the result of merging the existing MDC context and the enclosing trace stored in MDC.
  //MDC is cleaned up afterwards.
  //Escape hatch for spots that interact directly with MDC, beyond our control.
  def withTraceInMdc[F[_]: Trace: Sync, G[_]: SyncEffect, A](
    preModifyOldContext: F[Map[String, String]] => F[Map[String, String]]
  )(
    inContext: G[A]
  ): F[A] = {
    val runner = MDCLogging.mdc.mdcWithContext

    (preModifyOldContext(MDCLogging.mdc.get[F]), Trace[F].kernel.map(_.toHeaders))
      .mapN(_ ++ _)
      .flatMap { ctx =>
        Sync[F].delay(
          runner.runWithContext(ctx)(
            SyncEffect[G].runSync[SyncIO, A](inContext).unsafeRunSync()
          )
        )
      }
  }

  //Same as withTraceInMdc but ignores (doesn't even ask for) the old context.
  def withSetTraceInMdc[F[_]: Trace: Sync, G[_]: SyncEffect, A](inContext: G[A]): F[A] =
    withTraceInMdc[F, G, A](Function.const(Map.empty[String, String].pure[F]))(inContext)

  implicit val tracedLogger: MessageLogger[Traced] =
    loggerBy[Traced](rawLogger.mapK(Kleisli.liftK))(
      (Kleisli(_.kernel): Traced[Kernel]).map(_.toHeaders)
    )

  def kleisliTracedClient[F[_]: Bracket[*[_], Throwable]: Defer](
    underlying: Client[F]
  ): Client[Kleisli[F, Span[F], *]] =
    Client { tracedReq =>
      type TracedF[A] = Kleisli[F, Span[F], A]

      val inNewSpan: TracedF ~> TracedF =
        λ[TracedF ~> TracedF](Trace[TracedF].span(tracedReq.uri.path)(_))

      val tracedResponseResource: TracedF[Resource[TracedF, Response[TracedF]]] =
        Kleisli { span =>
          span.kernel.map(_.toHeaders.map { case (k, v) => Header(k, v) }.toSeq).map {
            headers =>
              val appliedRequest =
                tracedReq.mapK(Kleisli.applyK(span)).withHeaders(headers: _*)

              underlying
                .run(appliedRequest)
                .map(_.mapK(Kleisli.liftK[F, Span[F]]))
                .mapK(Kleisli.liftK[F, Span[F]])
          }
        }

      Resource.suspend(inNewSpan(tracedResponseResource))
    }

  def run(args: List[String]): IO[ExitCode] = {
    for {
      blocker <- Blocker[IO]
      implicit0(client: Client[Traced]) <- BlazeClientBuilder[IO](ExecutionContext.global)
                                            .resource
                                            .map(kleisliTracedClient(_))
      entryPoint <- Zipkin.entryPoint[IO]("client", blocker)
      // entryPoint = Log.entryPoint[IO]("client")
    } yield {
      val bl = BusinessLogic.instance[Traced]

      def exec(msg: String): IO[Unit] =
        entryPoint
          .root(msg)
          .use {
            Kleisli
              .liftF(IO(ju.UUID.randomUUID()).map(Args(_, msg)))
              .flatMap(bl.execute(_))
              .run
          }
          .void

      exec("hello") &> exec("bye")
    }
  }.use(_ *> IO.never).as(ExitCode.Success)
}

final case class Result(message: String)
final case class Args(requestId: ju.UUID, message: String)

object Args {
  implicit val show: Show[Args] = Show.fromToString
}

@finalAlg
trait BusinessLogic[F[_]] {
  def execute(args: Args): F[Result]
}

object BusinessLogic {

  def instance[F[_]: Sync: Timer: Client: Trace: MessageLogger]: BusinessLogic[F] = {

    val client = implicitly[Client[F]]
    val dsl = new org.http4s.client.dsl.Http4sClientDsl[F] {}

    val goodOldDumbLogger = Slf4jLogger.getLogger[SyncIO]

    import dsl._
    import org.http4s.Method._
    import org.http4s.implicits._

    new BusinessLogic[F] {
      def execute(args: Args): F[Result] =
        for {
          _ <- MessageLogger[F].info(show"Executing request $args")
          _ <- Trace[F].span("child-span")(Timer[F].sleep(100.millis))
          _ <- client.successful(POST(uri"http://localhost:8080/execute"))
          _ <- MessageLogger[F].info(show"Executed request $args")
          _ <- ReaderTracing.withSetTraceInMdc(
                goodOldDumbLogger.info(
                  "I'm a good old dumb logger but I have the trace context!"
                )
              )
        } yield Result(show"${args.message} finished")
    }
  }
}
