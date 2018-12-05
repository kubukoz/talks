package io.example.samples

import cats.{Applicative, Apply}
import cats.instances.option._
import cats.syntax.apply._

object ApplyUsage {
  val string: Option[String] = Some("hello")
  val number: Option[Int]    = Some(5)

  //these are equivalent
  val opResult1: Option[Boolean] = Apply[Option].map2(string, number)((str, len) => str.length == len)

  val opResult2: Option[Boolean] = (string, number).mapN((str, len) => str.length == len)



  //scroll down...










  //for an arbitrary Apply
  def checkLength[F[_]: Apply](fstr: F[String], fint: F[Int]): F[Boolean] = {
    (fstr, fint).mapN(_.length == _)
  }


  //None
  checkLength[Option](Some("foo"), None)

  //Some(true)
  checkLength[Option](Some("foo"), Some(3))

  //Some(false)
  checkLength[Option](Some("foo"), Some(5))
}
