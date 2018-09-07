val program1Right = readLn.flatMap(line => IO(println(line)))

val program2Right = {
  putStrLn("foo") *>
  putStrLn("bar")
}
