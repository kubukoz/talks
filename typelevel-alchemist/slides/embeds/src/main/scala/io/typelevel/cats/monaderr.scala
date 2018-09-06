trait MonadError[F[_], E] extends ApplicativeError[F, E] with Monad[F] {

  /**
    * Turns a successful value into an error
    * if it does not satisfy a given predicate.
    */
  def ensure[A](fa: F[A])(error: => E)(predicate: A => Boolean): F[A] =
    flatMap(fa)(a => if (predicate(a)) pure(a) else raiseError(error))

  /**
    * Transform certain errors using `pf` and rethrow them.
    * Non matching errors and successful values
    * are not affected by this function.
    */
  def adaptError[A](fa: F[A])(pf: PartialFunction[E, E]): F[A] =
    flatMap(attempt(fa))(_.fold(e => raiseError(pf.applyOrElse[E, E](e, _ => e)), pure))

  /**
    * Inverse of `attempt`.
    */
  def rethrow[A](fa: F[Either[E, A]]): F[A] =
    flatMap(fa)(_.fold(raiseError, pure))
}
