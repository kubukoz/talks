//in app
type RemainingNukes = Int

case class Console(println: String => IO[Unit])
case class Nuker(launchNukes: IO[RemainingNukes])

class App(console: Console, nuker: Nuker) {

  val run: IO[Unit] = nuker.launchNukes.flatMap { remaining =>
    console.println(s"Launched, remaining: $remaining")
  }
}

//in tests
def testConsole(lines: Ref[IO, List[String]]): Console =
  Console(s => lines.modify(s :: _).void)

def testNuker(nukes: Ref[IO, Int]): Nuker =
  Nuker(nukes.modify(_ - 1).map(_.now))

val testApp = for {
  lines <- Ref[IO, List[String]](Nil)
  nukes <- Ref[IO, Int](10)
} yield new App(testConsole(lines), testNuker(nukes))

val test = for {
  app    <- testApp
  _      <- app.run
  lines1 <- lines.get
  nukes1 <- nukes.get

  _      <- app.run
  lines2 <- lines.get
  nukes2 <- nukes.get
} yield {
  lines1 shouldBe List("Launched, remaining: 9")
  nukes1 shouldBe 9

  lines2 shouldBe List("Launched, remaining: 8",
                       "Launched, remaining: 9")
  nukes2 shouldBe 8
}

test.unsafeRunSync()
