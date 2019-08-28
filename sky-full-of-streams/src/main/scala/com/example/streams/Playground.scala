package com.example.streams

import cats.effect._
import cats.~>

import scala.concurrent.duration._
import scala.util.Random
import cats.data.`package`.State

object Playground extends IOApp {

  import fs2.Stream
  /*
  val pureStream = Stream(1, 3, 5)

  val calculatedStream = Stream.iterate(0)(_ + 1)

  val fromList = Stream.emits(List("foo", "bar", "baz"))

  val fromEffect = Stream.eval(IO(Random.nextInt()))

  val resource = Stream.bracket(IO( /* acquire resource */ ))(
    r => /* cleanup resource */ IO.unit
  ) */

  val scheduled = Stream.awakeDelay[IO](1.second)

  //alternative approach:

  import cats.implicits._

  val now = IO(System.currentTimeMillis())

  import scala.util.Random

  val exponentialWait =
    Stream.iterate(1.0)(_ * 1.15).map(_.millis).evalMap(IO.sleep)

  val stars = Stream
    .emits(1 to 40)
    .evalMap { n =>
      IO { Random.nextInt(n * 2) }
    }
    .zipLeft(exponentialWait)
    .flatMap { a =>
      Stream.emits(0 to a).map(_ => "*").reduce(_ ++ _)
    } ++ Stream.emit("The end").covary[IO].showLinesStdOut.drain

  val stepByStep = Stream
    .eval(now)
    .flatMap { start =>
      Stream(1, 2, 3)
        .flatMap { elem =>
          Stream(0, 1, 2).map(elem + _)
        }
        .evalMap { elem =>
          IO.sleep(elem * 100.millis) *> now.map(
            later => (later - start) / 100
          )
        }
        .zipLeft(Stream.iterate(10)(_ - 1))
    }
    .compile
    .toList

  import cats.effect.Console.io._

  def run(args: List[String]): IO[ExitCode] =
    stepByStep
      .flatMap(_.traverse(putStrLn(_)))
      .map(_ => ExitCode.Success)
}
