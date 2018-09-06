class TimeService[F[_]](implicit F: Sync[F]) {

  val showTime: F[Unit] = for {
    time <- F.delay(System.currentTimeMillis())
    _    <- F.delay(println(time))
  } yield ()
}

val ts = new TimeService[IO]

ts.showTime.unsafeRunSync()
