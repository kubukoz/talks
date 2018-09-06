val now: Future[Long] = Future(System.currentTimeMillis())

def measure[T](f: => Future[T]): Future[(T, Long)] =
  for {
    before <- now
    value  <- f
    after  <- now
  } yield (value, after - before)

measure(Future(Thread.sleep(1000))) //always 0

val f = Future(Thread.sleep(1000))

measure(f) //0
measure(f) //0
