package io.example.rt

import cats.effect.{ExitCode, IO, IOApp}
import cats.syntax.functor._

object Main extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    IO(println("Foo!")).as(ExitCode.Success)
}
