import cats.effect.IO
import cats.implicits._
import scala.io.StdIn

val x = IO(StdIn.readLine())

//(x, x) == (IO(StdIn.readLine()), IO(StdIn.readLine()))

(x >> x) == (IO(StdIn.readLine()) >> IO(StdIn.readLine()))
