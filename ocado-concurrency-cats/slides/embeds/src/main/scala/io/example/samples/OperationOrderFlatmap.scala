package io.example.samples

import cats.syntax.apply._
import io.example.fake.realer.FlatMap

object OperationOrderFlatmap {

  def whatOrder[F[_]: FlatMap, A, B](f1: F[A], f2: F[B]): F[B] = {
    //f1 *> f2 === map2(f1, f2)((_, b) => b)

    f1 *> f2 // === f1.flatMap(_ => f2) for monads

    //FlatMap also provides f1 >> f2
  }
}
