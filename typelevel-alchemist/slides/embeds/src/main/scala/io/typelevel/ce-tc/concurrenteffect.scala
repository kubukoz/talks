trait ConcurrentEffect[F[_]]
    extends Concurrent[F]
    with Effect[F] {

  def runCancelable[A](fa: F[A])(
    cb: Either[Throwable, A] => IO[Unit]): SyncIO[CancelToken[F]]
}
