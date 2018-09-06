val now: IO[Long] = IO(System.currentTimeMillis())

def measure[T](f: IO[T]): IO[(T, Long)] =
  for {
    before <- now
    value  <- f
    after  <- now
  } yield (value, after - before)

measure(IO.sleep(1000.millis)).unsafeRunSync() //~1000

val f = IO.sleep(1000.millis)

//don't unsafe at home
measure(f).unsafeRunSync() //~1000
measure(f).unsafeRunSync() //~1000
