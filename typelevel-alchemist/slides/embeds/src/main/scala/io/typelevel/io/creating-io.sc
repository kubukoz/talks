//wrap an eager, pure expression without side effects
IO.pure[A](a: A)

//constant for IO.pure(())
IO.unit

//wrap a side-effecting computation, lazily
IO.apply[A](a: => A)

//in `k` we can trigger an asynchronous call
//that'll execute the given callback upon completion
IO.asyncF[A](k: (Either[Throwable, A] => Unit) => IO[Unit]): IO[A]

//create a failed IO
IO.raiseError[A](e: Throwable): IO[A]
