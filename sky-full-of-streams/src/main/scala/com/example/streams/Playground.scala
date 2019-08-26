package com.example.streams

import cats.effect._

object Playground extends IOApp {

  import fs2.Stream

  val pureStream = Stream(1, 3, 5)

  val calculatedStream = Stream.iterate(0)(_ + 1)

  def run(args: List[String]): IO[ExitCode] =
    ???
}
