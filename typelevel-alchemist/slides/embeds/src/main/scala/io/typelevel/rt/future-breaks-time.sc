def now(): Future[Long] =
  Future(System.currentTimeMillis())

def measure[T](f: => Future[T]): Future[(T, Long)] =
  for {
    before <- now()
    value  <- f
    after  <- now()
  } yield (value, after - before)

measure(Future(Thread.sleep(1000))) //more or less 1000

val f = Future(Thread.sleep(1000))

measure(f) //less than 1000
measure(f) //even less because `f` is already running
