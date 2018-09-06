import cats.effect.{ExitCode, IO}

object Main extends IOApp {

  def run(args: List[String]): IO[ExitCode] = {
    val ioa = IO { println("hey!") }

    val program: IO[Unit] =
      for {
        _ <- ioa
        _ <- ioa
      } yield ()

    program.as(ExitCode.Success)
  }
}
