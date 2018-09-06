def dropDatabase(): Unit //or Future[Unit], or whatever

def getLength(arg: String): Int = {
  dropDatabase()
  arg.length
}

val helloLength = getLength("hello") //oh crap