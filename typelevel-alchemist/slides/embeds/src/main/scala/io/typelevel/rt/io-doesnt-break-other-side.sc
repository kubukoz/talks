val launchNukes: IO[Unit]

//nukes twice
val myProgram: IO[Unit] = {
  val result = launchNukes

  result *> result *> IO(println("foo")) *> IO(println("bar"))
}

//inline `result`
//still nukes twice
val myProgram2: IO[Unit] =
  launchNukes *> launchNukes *>
    IO(println("foo")) *> IO(println("bar"))
