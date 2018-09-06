trait Async[F[_]] extends Sync[F] with LiftIO[F] {

  def asyncF[A](
    k: (Either[Throwable, A] => Unit) => F[Unit]): F[A]
}
