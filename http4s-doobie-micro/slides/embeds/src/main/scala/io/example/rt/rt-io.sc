val x = IO(StdIn.readLine())

//(x, x) ?? (IO(StdIn.readLine()), IO(StdIn.readLine()))

//a >> b is a.flatMap(_ => b)
(x >> x) <-> (IO(StdIn.readLine()) >> IO(StdIn.readLine()))
