trait ApplicativeError[F[_], E] extends Applicative[F] {

  /**
    * Lift an error into the `F` context.
    */
  def raiseError[A](e: E): F[A]

  /**
    * Handle any error, potentially recovering from it,
    * by mapping it to an `F[A]` value.
    */
  def handleErrorWith[A](fa: F[A])(f: E => F[A]): F[A]

  def attempt[A](fa: F[A]): F[Either[E, A]] =
    fa.map(Right(_))
      .handleErrorWith(e => pure(Left(e)))
}
