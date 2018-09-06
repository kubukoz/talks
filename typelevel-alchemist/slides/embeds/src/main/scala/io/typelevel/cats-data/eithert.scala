final case class EitherT[F[_], A, B](value: F[Either[A, B]]) {

  def map[D](f: B => D)(
    implicit F: Functor[F]): EitherT[F, A, D] =
  /*provided by cats*/

  def flatMap[AA >: A, D](f: B => EitherT[F, AA, D])(
    implicit F: Monad[F]): EitherT[F, AA, D] =
  /*provided by cats*/
}
