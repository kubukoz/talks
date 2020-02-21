package com.kubukoz.tracing.mdc

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
import cats.effect.implicits._
import kamon.http4s.middleware.client.KamonSupport
import cats.effect.Sync
import kamon.Kamon
import kamon.tag.TagSet
import cats.effect.Resource
import cats.effect.ExitCase
import cats.effect.Async
import cats.effect.Clock
import kamon.context.Context
import kamon.context.Storage.Scope
import kamon.trace.SpanBuilder

@finalAlg
trait Tracing[F[_]] {
  def keepSpanAround[A](fa: F[A]): F[A]
  def inSpan[A](span: Span)(fa: F[A]): F[A]
}

object Tracing {
  def println(s: String) = Console.println(Thread.currentThread().getName() + ": " + s)
  implicit def kamonInstance[F[_]: Async: ContextShift]: Tracing[F] = new Tracing[F] {

    object raw {
      val currentCtx = Sync[F].delay(Kamon.currentContext())
      val currentSpan = Sync[F].delay(Kamon.currentSpan())
      def store(ctx: Context) = Sync[F].delay(Kamon.storeContext(ctx))
      def close(ctx: Scope) = Sync[F].delay(ctx.close())
      def start(spanBuilder: SpanBuilder) = Sync[F].delay(spanBuilder.start())

      def finishCase(span: kamon.trace.Span, ec: ExitCase[Throwable]) =
        Sync[F].delay {
          ec match {
            case ExitCase.Completed => span.finish()
            case ExitCase.Error(e)  => span.fail(e)
            case ExitCase.Canceled  => span.fail("Canceled")
          }
        }.void

      def shiftWith(ctx: Context): F[Unit] = Async[F].async[Unit] { cb =>
        Kamon.runWithContext(ctx) {
          cb(Right(()))
        }
      }
    }

    def keepSpanAround[A](fa: F[A]): F[A] =
      //shift afterwards to ensure we're not on `fa`'s thread anymore,
      //which would mean everything after `keepSpanAround` executes on that thread.
      raw.currentCtx.flatMap(fa.guarantee(ContextShift[F].shift) <* raw.shiftWith(_))

    private def scope(ctx: Context): Resource[F, Unit] =
      Resource.make(raw.store(ctx))(raw.close).void

    def inSpan[A](span: Span)(fa: F[A]): F[A] = {
      val buildSpan = raw.currentSpan.map { parent =>
        Kamon.spanBuilder(span.name).tag(TagSet.from(span.toMap)).asChildOf(parent)
      }

      val kamonSpan = Resource.makeCase(buildSpan.flatMap(raw.start))(raw.finishCase)

      kamonSpan
        .flatTap { span =>
          Resource.suspend(
            raw.currentCtx.map(_.withEntry(kamon.trace.Span.Key, span)).map(scope)
          )
        }
        .use(_ => fa)
    }
  }
}

final class KamonExecutionContext(underlying: ExecutionContext) extends ExecutionContext {

  def execute(runnable: Runnable): Unit = {
    val ctx = Kamon.currentContext()

    underlying.execute { () =>
      Kamon.runWithContext(ctx)(runnable.run())
    }
  }
  def reportFailure(cause: Throwable): Unit = underlying.reportFailure(cause)
}

trait Init {
  //has to be done before logger is initialized
  System.setProperty("APP_NAME", "%magenta(client  )")
  Kamon.init()
}

object MDCTracing extends Init with IOApp {

  val executionContext = new KamonExecutionContext(ExecutionContext.global)
  override implicit val contextShift: ContextShift[IO] = IO.contextShift(executionContext)
  override implicit val timer: Timer[IO] = new Timer[IO] {
    private val underlying = IO.timer(executionContext)

    val clock: Clock[IO] = underlying.clock

    def sleep(duration: FiniteDuration): IO[Unit] =
      Tracing[IO].keepSpanAround(underlying.sleep(duration))
  }

  def run(args: List[String]): IO[ExitCode] = {
    BlazeClientBuilder[IO](executionContext).resource.map(KamonSupport(_)).use {
      implicit client =>
        val bl = BusinessLogic.instance

        def exec(msg: String) =
          Span.create(msg).flatMap { span =>
            Tracing[IO].inSpan(span) {
              IO(ju.UUID.randomUUID()).map(Args(_, msg)).flatMap(bl.execute)
            }
          }

        exec("hello") // &> exec("bye")
    }
  }.as(ExitCode.Success)
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

  def instance(
    implicit cs: ContextShift[IO],
    timer: Timer[IO],
    client: Client[IO],
    tracing: Tracing[IO]
  ): BusinessLogic[IO] = {
    val logger = Slf4jLogger.getLogger[IO]

    import org.http4s.client.dsl.io._
    import org.http4s.Method._
    import org.http4s.implicits._

    new BusinessLogic[IO] {
      def execute(args: Args): IO[Result] =
        for {
          _ <- logger.info(show"Executing request $args")
          _ <- IO.sleep(100.millis)
          _ <- logger.info("before: " + Kamon.currentSpan().operationName())
          _ <- Tracing[IO].keepSpanAround(
                client.successful(POST(uri"http://localhost:8080/execute"))
              )
          _ <- logger.info("after: " + Kamon.currentSpan().operationName())
          _ <- logger.info(show"Executed request $args")
        } yield Result(show"${args.message} finished")
    }
  }
}
