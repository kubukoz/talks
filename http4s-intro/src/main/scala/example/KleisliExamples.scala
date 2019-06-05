package example

import cats.effect.IO
import cats.data.Kleisli
import cats.implicits._

object KleisliExamples {
  type Token = String

  val len: Kleisli[IO, Token, Int] = Kleisli { token =>
    IO.pure(token.length)
  }

  val cloned: Kleisli[IO, Token, String] = Kleisli { token =>
    IO.pure(token + token)
  }

  val f3 = for {
    a <- len
    b <- cloned
  } yield (a + b)

  val f4 = (len, cloned).tupled
}
