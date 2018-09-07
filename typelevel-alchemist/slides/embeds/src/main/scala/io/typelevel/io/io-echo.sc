import cats.effect.IO
import cats.implicits._
import scala.io.StdIn

val readLn: IO[String] =
  IO(StdIn.readLine())

def putStrLn(s: String): IO[Unit] = IO(println(s))

val program = for {
  _    <- putStrLn("What's your name?")
  name <- readLn
  _    <- putStrLn(show"Your name was $name")
} yield ()
