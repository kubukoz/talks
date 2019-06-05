package example

import cats.effect.IO
import cats.data.OptionT
import cats.implicits._

object OptionTExamples {
  val num: OptionT[IO, Int] = OptionT(IO.pure(42.some))

  def text(num: Int): OptionT[IO, String] = num match {
    case n if n % 2 === 0 => OptionT(IO.pure("foo".some))
    case _ => OptionT.none
  }

  val result = for {
    n  <- num
    s  <- text(n)
    s2 <- text(n + 1)
    s3 <- text(n + 2)
  } yield s + s2 + s3

  val unwrapped: IO[Option[String]] = result.value
}
