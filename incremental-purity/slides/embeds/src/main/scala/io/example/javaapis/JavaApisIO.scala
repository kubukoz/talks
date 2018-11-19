package io.example.javaapis
import java.io.{BufferedReader, File, FileReader}

import cats.effect.IO
import scala.collection.JavaConverters._

object JavaApisIO {

  private def readAllLines(bufferedReader: BufferedReader): List[String] = {
    bufferedReader.lines().iterator().asScala.toList
  }

  private def reader(file: File): BufferedReader =
    new BufferedReader(new FileReader(file))

  def readLinesFromFile(file: File): IO[List[String]] = IO {
    val r = reader(file)

    try {
      readAllLines(r)
    } finally {
      r.close()
    }
  }
}
