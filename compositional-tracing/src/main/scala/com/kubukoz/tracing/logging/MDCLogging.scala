package com.kubukoz.tracing.logging

import org.slf4j.LoggerFactory
import java.{util => ju}
import scala.util.Random
import org.slf4j.MDC
import scala.concurrent.ExecutionContext
import cats.effect.IO
import cats.implicits._
import cats.effect.IOApp
import cats.effect.ContextShift
import cats.effect.ExitCode

import scala.concurrent.duration._
import cats.effect.Timer
import cats.effect.Clock
import cats.~>
import com.kubukoz.tracing.ContextKeeper
import com.kubukoz.tracing.ContextKeeper.UnsafeRunWithContext
import cats.effect.Sync

object MDCLogging extends IOApp {
  val logger = LoggerFactory.getLogger(getClass())

  object mdc {
    val ec = mdc.mdcAware(ExecutionContext.global)

    import scala.jdk.CollectionConverters._

    private def grab() =
      Option(MDC.getCopyOfContextMap())
        .map(_.asScala.toMap)
        .getOrElse(Map.empty[String, String])

    def get[F[_]: Sync]: F[Map[String, String]] = Sync[F].delay(grab())

    val mdcWithContext: UnsafeRunWithContext[Map[String, String]] =
      new UnsafeRunWithContext[Map[String, String]] {

        def runWithContext[A](ctx: Map[String, String])(f: => A): A = {
          val childContext = grab()

          MDC.setContextMap(ctx.asJava)

          try {
            f
          } finally {
            MDC.setContextMap(childContext.asJava)
          }
        }
      }

    def mdcAware(underlying: ExecutionContext): ExecutionContext = new ExecutionContext {

      def execute(runnable: Runnable): Unit = {

        val rootContext = grab()

        underlying.execute { () =>
          mdcWithContext.runWithContext(rootContext) {
            runnable.run()
          }
        }
      }

      def reportFailure(cause: Throwable): Unit = underlying.reportFailure(cause)
    }

    implicit val contextShift: ContextShift[IO] = IO.contextShift(ec)

    implicit val keeper: ContextKeeper[IO, Map[String, String]] = ContextKeeper.instance(
      mdcWithContext,
      get[IO]
    )

    val timer: Timer[IO] = new Timer[IO] {
      val underlying = IO.timer(ec)

      def clock: Clock[IO] = underlying.clock

      def sleep(duration: FiniteDuration): IO[Unit] =
        keepContextAround(underlying.sleep(duration))
    }

    val keepContextAround: IO ~> IO =
      keeper.keepContextAround

    def runIOWithContext: Map[String, String] => IO ~> IO =
      keeper.withContext
  }

  override implicit val contextShift: ContextShift[IO] = mdc.contextShift

  override implicit val timer: Timer[IO] = mdc.timer

  type Result = Int

  val processPayment: String => IO[Result] = _ =>
    IO.shift *> IO {
      Random.nextInt(100)
    } <* IO.sleep(100.millis)

  def executeRequest(paymentId: String) =
    (mdc.get[IO], IO(ju.UUID.randomUUID().toString()))
      .mapN((ctx, requestId) => ctx + ("RequestId" -> requestId))
      .flatMap {
        mdc.runIOWithContext(_) {
          IO(logger.info(s"Started processing payment $paymentId")) *>
            processPayment(paymentId).flatMap { result =>
              IO(logger.info(s"Finished processing payment with result $result"))
            }
        }
      } *> IO(logger.info("out of context"))

  def run(args: List[String]): IO[ExitCode] = {
    (1 to 4).map(_.toString).toList.parTraverse(executeRequest)
  }.as(ExitCode.Success)

}
