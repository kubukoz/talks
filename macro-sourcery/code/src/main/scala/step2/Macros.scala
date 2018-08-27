package step2

import scala.language.experimental.macros
import scala.reflect.macros.blackbox

object Macros {
  type DDFunction = Double => Double

  /**
    * derive(x => -2 * (math.pow(x, 2) + 4 * x - 5)) == -4 * x - 8
    **/
  def derive(f: DDFunction): DDFunction = ???
}