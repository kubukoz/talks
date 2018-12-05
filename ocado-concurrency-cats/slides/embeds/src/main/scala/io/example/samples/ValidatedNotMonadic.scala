package io.example.samples
import cats.Apply
import cats.data.{Validated, ValidatedNec}
import cats.implicits._

object ValidatedNotMonadic {
  val v1: ValidatedNec[String, Int]     = "foo".invalidNec
  val v2: ValidatedNec[String, Boolean] = "bar".invalidNec

  //let's try defining flatMap
  def flatMapV[A, B, E](fa: Validated[E, A])(f: A => Validated[E, B]): Validated[E, B] =
    fa.andThen(f)

  //for monads, we can define map2 with flatMap
  def map2V[A, B, C, E](fa: Validated[E, A], fb: Validated[E, B])(f: (A, B) => C): Validated[E, C] =
    flatMapV(fa)(a => fb.map(f(a, _)))

  val instance = Apply[ValidatedNec[String, ?]]
  import instance.map2

  val left = map2V(v1, v2)(Tuple2.apply)

  val right = map2(v1, v2)(Tuple2.apply)

  //false!
  left == right

  //left: Invalid(NonEmptyChain("foo"))
  //right: Invalid(NonEmptyChain("foo", "bar"))
}
