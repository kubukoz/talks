package io.example.javaapis

import java.io._
import collection.JavaConverters._
import cats.effect._

object JavaApisResource {

  def readAllLines(bufferedReader: BufferedReader): IO[List[String]] = IO {
    bufferedReader.lines().iterator().asScala.toList
  }

  def reader(file: File): Resource[IO, BufferedReader] =
    Resource.make(IO(new BufferedReader(new FileReader(file))))(br => IO(br.close()))

  def readLinesFromFile(file: File): IO[List[String]] = {
    reader(file).use(readAllLines)
  }
}
