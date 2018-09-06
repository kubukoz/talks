def launchNukes(): Future[Unit]

//nukes once
def myProgram() = {
  val result = launchNukes()

  result.foreach(_ => println("foo"))
  result.foreach(_ => println("bar"))
}

//inline `result`
//nukes twice
def myProgram2() = {
  launchNukes().foreach(_ => println("foo"))
  launchNukes().foreach(_ => println("bar"))
}
