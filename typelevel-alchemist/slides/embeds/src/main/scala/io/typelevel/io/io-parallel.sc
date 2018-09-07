val io1: IO[Int] = IO.sleep(1.second) *> IO.pure(42)
val io2: IO[String] = IO.sleep(2.seconds) *> IO.pure("a" * 58)

//takes 2 seconds
val sum: IO[Int] = (io1, io2).parMapN(_ + _.length)

//race the IOs, cancel the loser (if possible)
//takes 1 second and cancels the second sleep
//eventually Left(42)
IO.race(io1, io2): IO[Either[Int, String]]
