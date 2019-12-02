package com.example.work

import cats.effect.IO
import cats.effect.IOApp
import cats.effect.ExitCode
import cats.implicits._
import scala.util.Random
import scala.concurrent.duration._
import cats.effect.Fiber
import cats.effect.Console.io._

sealed trait Results

object Results {
  case object GoodCode extends Results
  case object BarelyUsableCode extends Results
}

final case class WorkContext(makesSense: Boolean)

object Developer {

  def create(performWork: WorkContext => IO[Results]): IO[Results] = {
    val ctx = WorkContext(makesSense = true)

    val prepareToWork: IO[WorkContext] =
      putStrLn("Preparing to work!") *> IO.pure(ctx)

    val stopThinkingAboutWork: WorkContext => IO[Unit] = _ => putStrLn("Stopping work!")

    prepareToWork.bracket(use = performWork)(
      release = stopThinkingAboutWork
    ) <* putStrLn("Really finished work!")
  }
}

trait Colleague {
  def interrupt[A](process: Fiber[IO, A])(msg: String): IO[Unit]
}

object Colleague {

  val friendly: Colleague = new Colleague {

    def interrupt[A](process: Fiber[IO, A])(msg: String): IO[Unit] =
      putStrLn(msg) *> process.cancel
  }
}

object Program extends IOApp {

  def run(args: List[String]): IO[ExitCode] = {
    val developer = Developer.create { ctx =>
      IO.sleep(5.seconds) *>
        IO(Random.nextBoolean() && ctx.makesSense).map {
          case true  => Results.GoodCode
          case false => Results.BarelyUsableCode
        }
    }

    val colleague = Colleague.friendly

    // format: off
    for {
      workerProcess <- developer.start
      _             <- IO.sleep(2.seconds)
      _             <- colleague.interrupt(workerProcess)(
                         msg = "Hey, can you do this one thing"
                       )
      result        <- workerProcess.join.timeout(5.seconds)
      _             <- putStrLn("Worker finished with result: " + result)
    } yield ()
    // format: on
  } as ExitCode.Success
}
