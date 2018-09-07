val program = for {
  ref <- Ref[IO].of(0)

  _    <- ref.update(_ + 1)
  fib1 <- ref.set(0).start
  fib2 <- ref.update(_ + 5).start

  _         <- fib1.join
  _         <- fib2.join
  lastValue <- ref.get
} yield lastValue //0 or 5
