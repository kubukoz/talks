trait Monad[F[_]] extends Applicative[F] {

  /**
    * Applies an effectful function f inside the context F,
    * and flattens the result to a single F[B].
    * */
  def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B]
}
