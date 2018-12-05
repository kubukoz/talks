package io.example.samples

import cats.Applicative
import cats.instances.option._
import cats.syntax.apply._

object ApplicativeUsage {
  val string: Option[String] = Applicative[Option].pure("hello")
  val number: Option[Int]    = Some(5)

  //these lines are equivalent
  val opResult1: Option[Boolean] = Applicative[Option].map2(string, number)((str, len) => str.length == len)

  //mapN works for any arity of 2-22 (tuple limit)
  val opResult2: Option[Boolean] = (string, number).mapN((str, len) => str.length == len)



  //scroll down...










  //for an arbitrary Applicative
  def checkLength[F[_]: Applicative](fstr: F[String], fint: F[Int]): F[Boolean] = {
    (fstr, fint).mapN(_.length == _)
  }


  //None
  checkLength[Option](Some("foo"), None)

  //Some(true)
  checkLength[Option](Some("foo"), Some(3))

  //Some(false)
  checkLength[Option](Some("foo"), Some(5))
}
