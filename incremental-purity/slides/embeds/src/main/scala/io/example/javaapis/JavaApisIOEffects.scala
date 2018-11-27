package io.example.javaapis

import java.io.{BufferedReader, File, FileReader}

import cats.effect.IO
import scala.collection.JavaConverters._
import cats.implicits._

object JavaApisIOEffects {

  private def readAllLines(bufferedReader: BufferedReader): IO[List[String]] = IO {
    bufferedReader.lines().iterator().asScala.toList
  }

  def visitWebsite(url: String): IO[Unit] = ???

  private def reader(file: File): BufferedReader =
    new BufferedReader(new FileReader(file))

  def readLinesFromFile(file: File): IO[List[String]] = {
    IO(reader(file)).flatMap { r =>
      try {
        readAllLines(r)
          .flatMap(_.traverse(line => visitWebsite(line).as(line)))
      } finally {
        //nope nope nope
        r.close()
      }
    }

  }
}
