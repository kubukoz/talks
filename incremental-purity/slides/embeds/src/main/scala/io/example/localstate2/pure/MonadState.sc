trait MonadState[F[_], S] extends Serializable {
  val monad: Monad[F]

  def get: F[S]

  def set(s: S): F[Unit]

  def inspect[A](f: S => A): F[A]

  def modify(f: S => S): F[Unit]
}
