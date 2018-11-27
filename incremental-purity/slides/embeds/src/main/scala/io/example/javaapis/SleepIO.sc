<span class="fragment">def sleep(finiteDuration: FiniteDuration)
         (implicit scheduler: ScheduledExecutorService): IO[Unit] = {
  IO.async { callback =>
    <span class="fragment">val runnable = new Runnable { override def run(): Unit = callback(Right(())) }</span>

    <span class="fragment">scheduler.schedule(runnable, finiteDuration.toMillis, TimeUnit.MILLISECONDS)</span>
  }
}</span>
<span class="fragment">
//more like the real version
def sleepCancelable(finiteDuration: FiniteDuration)
                   (implicit scheduler: ScheduledExecutorService): IO[Unit] =
  IO.cancelable { callback =>
    val runnable = new Runnable { override def run(): Unit = callback(Right(())) }

    val scheduled = scheduler.schedule(runnable, finiteDuration.toMillis, TimeUnit.MILLISECONDS)

    IO {
      scheduled.cancel(false)
    }
  }</span>
<span class="fragment">
//the real version, use this in real applications
//also works with time travelling `TestContext.tick()` and cancelation
def sleepStandard(finiteDuration: FiniteDuration)(implicit timer: Timer[IO]): IO[Unit] =
  timer.sleep(finiteDuration)</span>
