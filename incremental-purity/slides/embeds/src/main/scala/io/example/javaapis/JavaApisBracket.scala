package io.example.javaapis

import java.io._
import collection.JavaConverters._
import cats.effect._

object JavaApisBracket {

  def readAllLines(bufferedReader: BufferedReader): IO[List[String]] = IO {
    bufferedReader.lines().iterator().asScala.toList
  }

  def reader(file: File): IO[BufferedReader] =
    IO(new BufferedReader(new FileReader(file)))

  def readLinesFromFile(file: File): IO[List[String]] = {
    reader(file).bracket(readAllLines)(r => IO(r.close()))
  }
}
