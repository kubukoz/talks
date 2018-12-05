package io.example.samples
import cats.Apply
import cats.syntax.apply._

object OperationOrder {

  def whatOrder[F[_]: Apply, A, B](f1: F[A], f2: F[B]): F[B] = {
    //f1 *> f2 === map2(f1, f2)((_, b) => b)

    f1 *> f2
  }
}
