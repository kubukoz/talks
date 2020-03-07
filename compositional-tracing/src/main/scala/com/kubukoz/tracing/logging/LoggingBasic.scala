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
import com.kubukoz.tracing.common.UnsafeRunWithContext
import com.kubukoz.tracing.common

object LoggingBasic extends IOApp {
  val logger = LoggerFactory.getLogger(getClass())

  type Result = Int

  import scala.jdk.CollectionConverters._

  private def grab() =
    Option(MDC.getCopyOfContextMap())
      .map(_.asScala.toMap)
      .getOrElse(Map.empty[String, String])

  private def runWithContext[A](ctx: Map[String, String])(f: => A): A = {
    val childContext = grab()

    MDC.setContextMap(ctx.asJava)

    try {
      f
    } finally {
      MDC.setContextMap(childContext.asJava)
    }
  }

  val mdcWithContext: UnsafeRunWithContext[Map[String, String]] =
    new UnsafeRunWithContext[Map[String, String]] {

      def runWithContext[A](ctx: Map[String, String])(f: => A): A =
        LoggingBasic.runWithContext(ctx)(f)
    }

  def mdcAware(underlying: ExecutionContext): ExecutionContext = new ExecutionContext {

    def execute(runnable: Runnable): Unit = {

      val rootContext = grab()

      underlying.execute(() =>
        runWithContext(rootContext) {
          runnable.run()
        }
      )
    }

    def reportFailure(cause: Throwable): Unit = underlying.reportFailure(cause)
  }

  implicit val ec = mdcAware(ExecutionContext.global)
  override implicit val contextShift: ContextShift[IO] = IO.contextShift(ec)

  override implicit val timer: Timer[IO] = new Timer[IO] {
    val underlying = IO.timer(ec)

    def clock: Clock[IO] = underlying.clock

    def sleep(duration: FiniteDuration): IO[Unit] =
      keepContextAround(underlying.sleep(duration))
  }

  def keepContextAround[A](action: IO[A]): IO[A] =
    IO(grab()).flatMap(runIOWithContext(_)(action))

  def runIOWithContext[A](ctx: Map[String, String])(action: IO[A]): IO[A] =
    common.runFWithContext(mdcWithContext, IO(grab()))(ctx)(action)

  val processPayment: String => IO[Result] = _ =>
    IO.shift *> IO {
      Random.nextInt(100)
    } <* IO.sleep(100.millis)

  def executeRequest(paymentId: String) =
    IO.suspend {
      val requestId = ju.UUID.randomUUID().toString()

      val ctx = grab() + ("RequestId" -> requestId)

      runIOWithContext(ctx) {
        IO(logger.info(s"Started processing payment $paymentId")) *>
          processPayment(paymentId).flatMap { result =>
            IO(logger.info(s"Finished processing payment with result $result"))
          }
      }
    } *> IO(logger.info("oops"))

  def run(args: List[String]): IO[ExitCode] = {
    // (1 to 1).map(_.toString).toList.parTraverse(executeRequest)
    executeRequest("1")
  }.as(ExitCode.Success)

}
