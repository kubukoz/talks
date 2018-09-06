val program1Wrong = readLn.map(line => println(line))

val program1Right = readLn.flatMap(line => IO(println(line)))

val program2Wrong = {
  putStrLn("foo")
  putStrLn("bar")
}

val program2Right = {
  putStrLn("foo") *>
  putStrLn("bar")
}
