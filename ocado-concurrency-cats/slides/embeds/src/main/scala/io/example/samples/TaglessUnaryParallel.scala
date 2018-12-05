package io.example.samples

import cats.implicits._
import cats.temp.par._

trait FooService2[F[_]] {
  def sum: F[Int]
}

object FooService2 {

  def make[F[_]: Par]: FooService[F] = new FooService[F] {
    val op1: F[Int] = ??? //can come from a dependency
    val op2: F[Int] = ???

    override val sum: F[Int] = (op1, op2).parMapN(_ + _)
  }
}
