final case class OptionT[F[_], A](value: F[Option[A]]) {

  def map[B](f: A => B)(implicit F: Functor[F]): OptionT[F, B] =
    /*provided by cats*/

  def flatMap[B](f: A => OptionT[F, B])(
    implicit F: Monad[F]
  ): OptionT[F, B] = /*provided by cats*/
}
