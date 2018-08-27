package com.kubukoz.examples

import cats.effect.Sync
import monix.eval.Task

import scala.concurrent.{Await, ExecutionContext, Future}
import concurrent.duration._
import cats.implicits._

object ReferentialTransparencyFutures {
  import ExecutionContext.Implicits.global

  def measure1[T](f: => Future[T]): Future[(T, Long)] = {
    def now() = System.currentTimeMillis()

    val start = now()
    for {
      result <- f
      end = now()
    } yield (result, end - start)
  }

  def measure2[T](f: => Future[T]): Future[(T, Long)] = {
    val now = System.currentTimeMillis()

    val start = now
    for {
      result <- f
      end = now
    } yield (result, end - start) //diff always zero - oops!
  }

  def measure3[T](f: => Future[T]): Future[(T, Long)] = {
    //that'll surely fix it, right?
    val now = Future { System.currentTimeMillis() }

    for {
      start  <- now
      result <- f
      end    <- now
    } yield (result, end - start) //nope, still zero
  }

  def measure4[T](f: Task[T]): Task[(T, Long)] = {
    //note: the argument is not by-name anymore

    val now = Task { System.currentTimeMillis() }

    //note: use this in real life instead
    //so that you can artificially pass the time in tests using TestScheduler
    //Task.timer.clockRealTime(TimeUnit.MILLISECONDS)

    //note: doesn't handle failure/cancelation cases for sake of example simplicity
    for {
      start  <- now
      result <- f
      end    <- now
    } yield (result, end - start) //fixed!
  }

  import scala.language.higherKinds

  def measure5[F[_]: Sync, T](f: F[T]): F[(T, Long)] = {
    //note: the argument is not by-name anymore

    val now = Sync[F].delay { System.currentTimeMillis() }

    for {
      start  <- now
      result <- f
      end    <- now
    } yield (result, end - start) //fixed!
  }

  private def awaitAndPrint[T](f: Future[T]): Unit = {
    val result = //don't do it in production, boi
      Await.result(f, 1.seconds)

    println(result)
  }


  private def awaitAndPrint[T](f: Task[T]): Unit = {
    import monix.execution.Scheduler.Implicits.global

    //don't do it in production, boi
    val result = f.toIO.unsafeRunSync()
    println(result)
  }

  def main(args: Array[String]): Unit = {

    def futureEcho(message: String): Future[String] = Future {
      Thread.sleep(100)
      message
    }

    def taskEcho(message: String): Task[String] = Task.sleep(100.millis) *> Task.pure(message)

    awaitAndPrint {
      measure1(futureEcho("Hello 1")) //future with time def
    }

    awaitAndPrint {
      measure2(futureEcho("Hello 2")) //future with time val
    }

    awaitAndPrint {
      measure3(futureEcho("Hello 3")) //future with future time
    }

    awaitAndPrint {
      measure4(taskEcho("Hello 4")) //task with time task
    }

    awaitAndPrint {
      measure5(taskEcho("Hello 5")) //same but decoupled from Task using Tagless Final
    }
  }
}
