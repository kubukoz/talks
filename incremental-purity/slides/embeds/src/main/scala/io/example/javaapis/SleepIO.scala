package io.example.javaapis
import java.util.concurrent.{ScheduledExecutorService, TimeUnit}

import cats.effect.{IO, Timer}

import scala.concurrent.duration.FiniteDuration

object SleepIO {

  def sleep(finiteDuration: FiniteDuration)(implicit scheduler: ScheduledExecutorService): IO[Unit] = {
    IO.async { callback =>
      val runnable = new Runnable { override def run(): Unit = callback(Right(())) }

      scheduler.schedule(runnable, finiteDuration.toMillis, TimeUnit.MILLISECONDS)
    }
  }

  //more like the real version
  def sleepCancelable(finiteDuration: FiniteDuration)(implicit scheduler: ScheduledExecutorService): IO[Unit] =
    IO.cancelable { callback =>
      val runnable = new Runnable { override def run(): Unit = callback(Right(())) }

      val scheduled = scheduler.schedule(runnable, finiteDuration.toMillis, TimeUnit.MILLISECONDS)

      IO {
        scheduled.cancel(false)
      }
    }

  //the real version, use this in real applications
  //also works with TestContext.tick()
  def sleepStandard(finiteDuration: FiniteDuration)(implicit timer: Timer[IO]): IO[Unit] = timer.sleep(finiteDuration)
}
