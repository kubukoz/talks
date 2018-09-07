import cats.effect.IO
import fs2._

val str: Stream[IO, String] = ???

//IO[Unit]
str.compile.drain

//IO[Unit]
str.evalTap[IO](s => IO(println(s))).compile.drain

//IO[List[String]], unsafe
str.compile.toList
