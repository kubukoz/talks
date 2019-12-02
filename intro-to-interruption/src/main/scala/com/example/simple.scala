package com.example

import cats.effect.Timer
import cats.effect.IO
import scala.concurrent.duration._
import cats.effect.Console.io._
import cats.implicits._
import cats.effect.ContextShift
import cats.effect.implicits._

class simple(implicit timer: Timer[IO], cs: ContextShift[IO]) {
  val action: IO[Unit] = putStrLn("Starting sleep") *> IO.sleep(3.seconds) *> putStrLn("After sleep")

  val program = for {
    forked <- action.start
    _      <- IO.sleep(1.second)
    _      <- forked.cancel
  } yield ()

  def primitivePar[A, B](a: IO[A], b: IO[B]): IO[(A, B)] =
    a.start
      .bracket { aFiber =>
        for {
          bV <- b.start.bracket(_.join)(_.cancel)
          aV <- aFiber.join
        } yield (aV, bV)
      }(_.cancel)

  def bothPar[A, B](a: IO[A], b: IO[B]): IO[(A, B)] = (a, b).parTupled
  def raceBoth[A, B](a: IO[A], b: IO[B]): IO[A Either B] = a race b

}
