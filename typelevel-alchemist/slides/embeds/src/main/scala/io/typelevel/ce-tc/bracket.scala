trait Bracket[F[_], E] extends MonadError[F, E] {

  //ExitCase[E] = Completed | Error[E] | Canceled
  def bracketCase[A, B](acquire: F[A])(use: A => F[B])(
    release: (A, ExitCase[E]) => F[Unit]): F[B]
}
