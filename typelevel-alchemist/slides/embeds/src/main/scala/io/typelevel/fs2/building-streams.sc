Stream(1, 2, 3)

Stream.constant("o", 3)

Stream.emit(1)

Stream.emits(List(1, 2, 3))

Stream.eval(IO(StdIn.readLine()))

Stream.iterate(0)(_ + 1)

Stream.iterateEval(0)(a => IO.sleep(1.second).as(a + 1))

Stream.random[IO]

Stream.repeatEval(IO(println("foo")))

Stream.unfoldEval(0) { offset =>
  getPage(offset).map {
    case page if page.isEmpty => None
    case page => Some((page, offset + page.size))
  }
}.flatMap(Stream.emits)
