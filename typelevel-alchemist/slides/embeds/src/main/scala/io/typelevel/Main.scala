package io.typelevel
import cats.effect._

object Main {
  def run(args: List[String]): IO[Unit] = {
    IO(println("hello world!"))
  }

  def main(args: Array[String]): Unit = {
    run(args.toList).unsafeRunSync()
  }
}
