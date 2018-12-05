package io.example.samples
import cats.data.Validated.Invalid
import cats.data.{NonEmptyChain, ValidatedNec, ValidatedNel}
import cats.syntax.validated._
import cats.syntax.apply._

object ParallelApplicatives {
//commented because now we use ValidatedNec

//  val v1: ValidatedNel[String, Int] = 5.valid
//  val v2: ValidatedNel[String, Boolean] = "foo".invalidNel

  //type ValidatedNec[+E, +A] = Validated[NonEmptyChain[E], A]
  //Chain - list optimized for concatenation

  val v1: ValidatedNec[String, Int]     = 5.valid
  val v2: ValidatedNec[String, Boolean] = "foo".invalidNec
  val v3: ValidatedNec[String, Unit]    = NonEmptyChain("bar", "baz").invalid

  val result: ValidatedNec[String, (Int, Boolean, Unit)] =
    (v1, v2, v3).tupled

  //true
  val b: Boolean = result == Invalid(NonEmptyChain("foo", "bar", "baz"))
}
