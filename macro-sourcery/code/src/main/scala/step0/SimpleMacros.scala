package step0

import reflect.macros.blackbox
import scala.language.experimental.macros

object SimpleMacros {
  def hello2(s: String): Unit = ???

  def getCode(a: Any): String = ???
}

object NonMacros {
  def hello(s: String) = println(s"Hello, $s!")
}