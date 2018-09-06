trait Applicative[F[_]] extends Functor[F] {

  /**
    * Given a value and a function in a context,
    * applies the function to the value.
    */
  def ap[A, B](ff: F[A => B])(fa: F[A]): F[B]

  /**
    * Given two values in the F context,
    * tuples them together in that context.
    */
  def product[A, B](fa: F[A], fb: F[B]): F[(A, B)] =
    ap(map(fa)(a => (b: B) => (a, b)))(fb)

  /**
    * `pure` lifts any value into the Applicative Functor.
    */
  def pure[A](x: A): F[A]
}
