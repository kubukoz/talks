package io.example.javaapis
import java.io.{BufferedReader, File, FileReader}

import scala.collection.JavaConverters._

object JavaApisImpure {

  def readAllLines(bufferedReader: BufferedReader): List[String] = {
    bufferedReader.lines().iterator().asScala.toList
  }

  def reader(file: File): BufferedReader =
    new BufferedReader(new FileReader(file))

  def readLinesFromFile(file: File): List[String] = {
    val r = reader(file)

    try {
      readAllLines(r)
    } finally {
      r.close()
    }
  }
}
