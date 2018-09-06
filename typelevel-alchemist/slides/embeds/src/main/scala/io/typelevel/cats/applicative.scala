trait Applicative[F[_]] extends Functor[F] {
  /**
    * Given two values in the F context,
    * tuples them together in that context.
    */
  def product[A, B](fa: F[A], fb: F[B]): F[(A, B)]

  /**
    * `pure` lifts any value into the Applicative Functor.
    */
  def pure[A](x: A): F[A]
}
