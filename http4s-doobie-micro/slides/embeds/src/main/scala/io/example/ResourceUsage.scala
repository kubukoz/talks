package io.example

import java.io.FileReader

import cats.implicits._
import cats.effect.{ExitCode, IO, IOApp, Resource}

//stubs
object Imports {
  def readLines(reader: FileReader): IO[String] = IO.pure(""".idea/
                                                            |*.iml
                                                            |*.iws
                                                            |*.eml
                                                            |out/""".stripMargin)
}

import Imports._

object ResourceUsage extends IOApp {

  def lock(name: String): Resource[IO, Unit] = {
    val acquire = IO(println(s"Acquiring $name"))
    val cleanup = IO(println(s"Releasing $name"))

    Resource.make(acquire)(_ => cleanup)
  }

  def file(name: String): Resource[IO, FileReader] = {
    val acquire = IO(println(s"Acquiring file reader: $name")) >> IO(new FileReader(name))

    def cleanup(fr: FileReader) = IO(println(s"Releasing file reader: $name")) >> IO(fr.close())

    Resource.make(acquire)(cleanup)
  }

  val megaResource: Resource[IO, FileReader] = for {
    _          <- lock("lock1")
    myResource <- file(".gitignore")
    _          <- lock("lock2")
  } yield myResource

  override def run(args: List[String]): IO[ExitCode] =
    megaResource
      .use(readLines(_) <* IO(println("Finished reading lines\n")))
      .flatMap(lines => IO(println(lines)))
      .as(ExitCode.Success)
}
