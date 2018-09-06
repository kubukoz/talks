val dropDatabase: IO[Unit] = IO {
  println("dropping db")
}

def getLength(arg: String): IO[Int] = {
  dropDatabase.map(_ => arg.length)
}

//oh well, this has effects in the type
val helloLength = getLength("hello")

//and nothing executes
//until we (or the runtime) call unsafeRunSync()
