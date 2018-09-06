val launchNukes: IO[Unit]

//nukes once
val myProgram: IO[Unit] = {
  val result = launchNukes

  result *> IO(println("foo")) *> IO(println("bar"))
}

//inline `result`
//still nukes once
val myProgram2: IO[Unit] =
  launchNukes *> IO(println("foo")) *> IO(println("bar"))
