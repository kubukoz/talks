val x = Future(StdIn.readLine())

//a >> b is a.flatMap(_ => b)

(x >> x) <!-> (Future(StdIn.readLine()) >> Future(StdIn.readLine()))
