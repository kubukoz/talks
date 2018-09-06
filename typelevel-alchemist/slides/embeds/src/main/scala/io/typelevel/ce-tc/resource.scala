sealed abstract class Resource[F[_], A] {

  def use[B](f: A => F[B])(
    implicit F: Bracket[F, Throwable]): F[B]
}
