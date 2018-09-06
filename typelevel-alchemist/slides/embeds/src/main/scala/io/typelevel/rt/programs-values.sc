def forever(action: IO[Unit]): IO[Unit] = {
  action >> forever(action)
}

def delay[T](action: IO[T],
             time: FiniteDuration): IO[T] = {
  IO.sleep(time) *> action
}

def timeout[T](action: IO[T],
               time: FiniteDuration): IO[T] = {
  action.race(IO.sleep(time)).flatMap {
    case Left(a)  => IO.pure(a)
    case Right(_) => IO.raiseError(new TimeoutException)
  }
}

def recoverEternal[T](action: IO[T]): IO[T] = {
  val tryAgain = IO.suspend(recoverEternal(action))
  action.handleErrorWith(_ => tryAgain)
}
