def sleepIO(d: FiniteDuration)(
  implicit ec: ScheduledExecutorService
): IO[Unit] = {
  IO.async { cb =>
    //no side effect here
    val run = new Runnable { def run(): Unit = cb(Right(())) }

    //side effect of scheduling suspended in IO
    IO {
      ec.schedule(run, d.length, d.unit)
    }.void
  }
}

def sleepF[F[_]: Async](d: FiniteDuration)(
  implicit ec: ScheduledExecutorService
): F[Unit] = {
    Async[F].asyncF { cb =>
      //no side effect here
      val run = new Runnable { def run(): Unit = cb(Right(())) }

      //side effect of scheduling suspended in F
      Sync[F].delay(ec.schedule(run, d.length, d.unit)).void
    }
  }
