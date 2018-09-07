trait Concurrent[F[_]] extends Async[F] {
  def start[A](fa: F[A]): F[Fiber[F, A]]

  def cancelable[A](
    k: (Either[Throwable, A] => Unit) => CancelToken[F])
    : F[A] = /*implemented in cats*/ ???
}

trait Fiber[F[_], A] {
  def cancel: CancelToken[F]
  def join: F[A]
}

type CancelToken[F[_]] = F[Unit]
