package com.kubukoz.tracing.magic

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
import kamon.http4s.middleware.client.KamonSupport
import cats.effect.Sync
import kamon.Kamon
import kamon.tag.TagSet
import cats.effect.Resource
import cats.effect.ExitCase
import cats.effect.Clock
import kamon.context.Context
import kamon.trace.SpanBuilder
import com.kubukoz.tracing.ContextKeeper
import kamon.context.Storage

@finalAlg
trait Tracing[F[_]] {
  def keepSpanAround[A](fa: F[A]): F[A]
  def inSpan[A](span: Span)(fa: F[A]): F[A]
}

object Tracing {

  def println(s: String) =
    Console.println(Thread.currentThread().getName() + ": " + s)

  implicit def kamonInstance[F[_]: Sync](
    implicit keeper: ContextKeeper[F, Context]
  ): Tracing[F] =
    new Tracing[F] {

      object raw {
        val currentCtx = Sync[F].delay(Kamon.currentContext())
        val currentSpan = Sync[F].delay(Kamon.currentSpan())
        def start(spanBuilder: SpanBuilder) = Sync[F].delay(spanBuilder.start())

        def finishCase(span: kamon.trace.Span, ec: ExitCase[Throwable]) =
          Sync[F].delay {
            ec match {
              case ExitCase.Completed => span.finish()
              case ExitCase.Error(e)  => span.fail(e)
              case ExitCase.Canceled  => span.fail("Canceled")
            }
          }.void
      }

      def keepSpanAround[A](fa: F[A]): F[A] = keeper.keepContextAround(fa)

      def inSpan[A](span: Span)(fa: F[A]): F[A] = {
        val buildSpan = raw.currentSpan.map { parent =>
          Kamon.spanBuilder(span.name).tag(TagSet.from(span.toMap)).asChildOf(parent)
        }

        val kamonSpan =
          Resource.makeCase(buildSpan.flatMap(raw.start))(raw.finishCase)

        kamonSpan
          .evalMap(span => raw.currentCtx.map(_.withEntry(kamon.trace.Span.Key, span)))
          .use(keeper.withContext(_)(fa))
      }
    }
}

final class KamonExecutionContext(underlying: ExecutionContext) extends ExecutionContext {

  def execute(runnable: Runnable): Unit = {
    val ctx = Kamon.currentContext()
    underlying.execute(() => Kamon.runWithContext(ctx)(runnable.run()))
  }

  def reportFailure(cause: Throwable): Unit = underlying.reportFailure(cause)
}

trait Init {
  //has to be done before logger is initialized
  System.setProperty("APP_NAME", "%magenta(client  )")
  System.setProperty("kamon.environment.service", "client")
  System.setProperty("kamon.context.debug", "true")
  Kamon.init()
}

trait KamonApp extends Init with IOApp {

  protected val executionContext = new KamonExecutionContext(ExecutionContext.global)

  override implicit val contextShift: ContextShift[IO] =
    IO.contextShift(executionContext)

  override implicit val timer: Timer[IO] = new Timer[IO] {
    private val underlying = IO.timer(executionContext)

    val clock: Clock[IO] = underlying.clock

    def sleep(duration: FiniteDuration): IO[Unit] =
      Tracing[IO].keepSpanAround(underlying.sleep(duration))
  }

  def runWithKamon(args: List[String]): IO[ExitCode]

  def run(args: List[String]): IO[ExitCode] =
    runWithKamon(args).guarantee(IO.fromFuture(IO(Kamon.stopModules())))
}

object KamonTracing extends KamonApp {

  def rightPad(len: Int, s: String): String =
    if (s.length > len) s.take(len)
    else s + " " * (len - s.length())

  val dumpKamon = IO(Storage.Debug.allThreadContexts()).map(_.toList).flatMap {
    _.traverse { thread =>
      IO(
        println(
          s"${rightPad(33, thread.thread.getName())} : ${thread.currentContext}"
        )
      )
    }
  }

  def runWithKamon(args: List[String]): IO[ExitCode] =
    BlazeClientBuilder[IO](executionContext).resource.map(KamonSupport(_)).use {
      implicit client =>
        val bl = BusinessLogic.instance

        def execSingle(msg: String) = {
          val makeCall = IO(ju.UUID.randomUUID()).map(Args(_, msg)).flatMap(bl.execute)

          Span.create[IO](msg).flatMap(Tracing.kamonInstance[IO].inSpan(_)(makeCall))
        }

        def exec(msg: String) =
          fs2
            .Stream
            .repeatEval {
              execSingle(msg).attempt <*
                IO(println("after: " + Kamon.currentSpan().operationName()))
            }
            .head
            .compile
            .drain

        show"aa"

        //run two in parallel, wait for both
        (exec("hello") &> exec("bye")) *> dumpKamon
    } *> (IO.sleep(1.second) *> IO( /*check for breakpoint*/ ()))
      .foreverM
      .as(ExitCode.Success)
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
    implicit
    timer: Timer[IO],
    client: Client[IO],
    tracing: Tracing[IO]
  ): BusinessLogic[IO] = {
    val logger = Slf4jLogger.getLogger[IO]

    import org.http4s.client.dsl.io._
    import org.http4s.Method._
    import org.http4s.implicits._

    def child[A](
      name: String,
      extraTags: Map[String, String] = Map.empty
    )(
      fa: IO[A]
    ): IO[A] =
      Span.create[IO](name).map(_.withValues(_ ++ extraTags)).flatMap {
        Tracing[IO].inSpan(_)(fa)
      }

    new BusinessLogic[IO] {
      private val databaseCall =
        child(
          "db-call",
          Map("db.query" -> "select * from users where id = ?")
        ) {
          logger.info("Running db call") *>
            IO.sleep(100.millis)
        }

      private val clientCall =
        child("remote-call") {
          Tracing[IO].keepSpanAround {
            client.successful(POST(uri"http://localhost:8080/execute"))
          }
        }

      def execute(args: Args): IO[Result] =
        for {
          _ <- logger.info(show"Executing request $args")
          _ <- databaseCall
          _ <- clientCall
          _ <- logger.info(show"Executed request $args")
        } yield Result(show"${args.message} finished")
    }
  }
}
