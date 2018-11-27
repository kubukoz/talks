package io.example.javaapis
import akka.actor.Scheduler

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.concurrent.duration.FiniteDuration

object SleepFuture {

  def sleep(duration: FiniteDuration)(implicit scheduler: Scheduler, ec: ExecutionContext): Future[Unit] = {
    val promise = Promise[Unit]()

    scheduler.scheduleOnce(duration) {
      promise.success(())
    }

    promise.future
  }
}
