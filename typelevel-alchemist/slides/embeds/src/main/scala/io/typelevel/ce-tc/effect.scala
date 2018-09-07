trait Effect[F[_]] extends Async[F] {

  def runAsync[A](fa: F[A])(
    cb: Either[Throwable, A] => IO[Unit]): SyncIO[Unit]

  def toIO[A](fa: F[A]): IO[A] =
    /*implemented in cats*/ ???
}
