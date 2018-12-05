package io.example.samples
import cats.effect._

import scala.concurrent.duration._
import cats.syntax.functor._

class IOStart(implicit timer: Timer[IO], contextShift: ContextShift[IO]) {
  val io1: IO[Int] = IO.sleep(2.seconds).as(5)

  val foo: IO[cats.effect.Fiber[IO, Int]] = io1.start

  //like parTupled, but doesn't cancel!
  val program: IO[(Int, Int)] = for {
    fiber1 <- io1.start
    fiber2 <- io1.start

    result1 <- fiber1.join
    result2 <- fiber2.join
  } yield (result1, result2)
}

trait Fiber[F[_], A] {
  def cancel: CancelToken[F] //type CancelToken[F[_]] = F[Unit]
  def join: F[A]
}
