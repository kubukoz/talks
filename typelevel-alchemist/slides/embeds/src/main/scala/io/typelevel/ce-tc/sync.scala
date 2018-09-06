trait Sync[F[_]] extends Bracket[F, Throwable] {
  def suspend[A](thunk: => F[A]): F[A]

  def delay[A](thunk: => A): F[A] = suspend(pure(thunk))
}

//Sync[IO].delay == IO.delay == IO.apply
