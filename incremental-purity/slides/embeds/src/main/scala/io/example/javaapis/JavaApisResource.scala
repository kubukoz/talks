package io.example.javaapis

import java.io._
import collection.JavaConverters._
import cats.effect._
import cats.implicits._

object JavaApisResource extends IOApp {

  def readAllLines(bufferedReader: BufferedReader): IO[List[String]] = IO {
    bufferedReader.lines().iterator().asScala.toList
  }

  def reader(file: File): Resource[IO, BufferedReader] = {
    val printOpening                    = IO(println(s"Opening file: $file"))
    val openReader                      = IO(new BufferedReader(new FileReader(file)))
    val printClosing                    = IO(println(s"Closing file: $file"))
    def closeReader(br: BufferedReader) = IO(br.close())

    Resource.make(printOpening *> openReader)(printClosing *> closeReader(_))
  }

  def lock(name: String): Resource[IO, Unit] =
    Resource.make(IO(println(s"Acquiring lock $name")))(_ => IO(println(s"Releasing lock $name")))

  def readLinesFromFile(file: File): IO[List[String]] = {
    val res = for {
      _          <- lock("1")
      fileReader <- reader(file)
      _          <- lock("2")
    } yield fileReader

    res.use(readAllLines)
  }

  override def run(args: List[String]): IO[ExitCode] = {
    val path = ".gitignore"
//    val path = ".gitignorefoo"
    for {
      lines <- readLinesFromFile(new File(path))
      _     <- IO(println("Finished reading lines"))
      _     <- lines.traverse(line => IO(println(line)))
    } yield ExitCode.Success
  }
}
