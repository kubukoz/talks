package io.example.samples

import cats.effect._
import cats.implicits._

import scala.concurrent.duration._
import cats.effect.Console.io._

object ParallelUsage extends IOApp {

  val job1: IO[String] = IO.sleep(2.seconds).as("hello world")

  val job2: IO[Int] = IO.sleep(3.seconds).as(11)

  val program: IO[Boolean] = (job1, job2).parMapN((str, len) => str.length == len)

  override def run(args: List[String]): IO[ExitCode] =
    program.flatMap(putStrLn(_)).as(ExitCode.Success)
}
