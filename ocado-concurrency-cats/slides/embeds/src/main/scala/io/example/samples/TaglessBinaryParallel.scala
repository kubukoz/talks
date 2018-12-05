package io.example.samples
import cats.Parallel
import cats.implicits._

trait FooService[F[_]] {
  def sum: F[Int]
}

object FooService {

  def make[F[_], G[_]](implicit par: Parallel[F, G]): FooService[F] = new FooService[F] {
    val op1: F[Int] = ??? //can come from a dependency
    val op2: F[Int] = ???

    override val sum: F[Int] = (op1, op2).parMapN(_ + _)
  }
}
