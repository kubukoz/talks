package io.example.samples
import cats.effect.{ContextShift, ExitCode, IO, IOApp}

import scala.concurrent.ExecutionContext

object ContextShiftCreation {
  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
}

object Main extends IOApp {
  //ContextShift with global EC available here

  override def run(args: List[String]): IO[ExitCode] =
    ???
}
