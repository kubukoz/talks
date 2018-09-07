trait Timer[F[_]]  {
  def clock: Clock[F]
  def sleep(duration: FiniteDuration): F[Unit]
}
