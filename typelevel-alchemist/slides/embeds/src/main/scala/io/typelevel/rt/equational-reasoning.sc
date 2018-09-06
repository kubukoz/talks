def launchNukes(nuker: String): IO[String] = {
  IO(println(s"$nuker launching nukes!")) *>
    IO.sleep(1.second) *>
    IO(println("launching nukes...")) *>
    IO.pure(nuker)
}

val getName          = IO(StdIn.readLine("What's your name?"))
val printBar         = IO(println("bar"))
val printFoo         = IO(println("foo"))
val getNameAndLaunch = getName.flatMap(launchNukes)

val program = printFoo *> getNameAndLaunch <* printBar