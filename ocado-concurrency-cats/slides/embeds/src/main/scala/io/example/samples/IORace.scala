package io.example.samples
import cats.effect.{ContextShift, IO, Timer}

import scala.concurrent.duration._
import cats.effect.implicits._
import cats.syntax.functor._

class IORace(implicit timer: Timer[IO], cs: ContextShift[IO]) {
  val io1: IO[Int]    = IO.sleep(2.seconds).as(5)
  val io2: IO[String] = IO.sleep(5.seconds).as("foo")

  //loser gets canceled
  val raced: IO[Either[Int, String]] = io1.race(io2)
}
