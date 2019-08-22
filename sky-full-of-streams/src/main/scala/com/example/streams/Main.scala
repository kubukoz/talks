package com.example.streams

import cats.effect.IOApp
import cats.effect.ExitCode
import cats.effect.IO
import cats.implicits._

object Main extends IOApp {

  def run(args: List[String]): IO[ExitCode] =
    IO {
      println("hello world")
    }.as(ExitCode.Success)
}
