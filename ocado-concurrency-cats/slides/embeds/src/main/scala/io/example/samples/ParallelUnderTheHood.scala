package io.example.samples
import cats.NonEmptyParallel

object ParallelUnderTheHood {

  def parProduct[M[_], F[_], A, B](ma: M[A], mb: M[B])(implicit P: NonEmptyParallel[M, F]): M[(A, B)] =
    P.sequential(
      P.apply.product(
        P.parallel(ma),
        P.parallel(mb)
      )
    )
}
