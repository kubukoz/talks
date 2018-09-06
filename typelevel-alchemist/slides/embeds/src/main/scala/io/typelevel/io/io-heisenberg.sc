import cats.effect.IO
import scala.io.StdIn
import cats.implicits._

val readLn: IO[String] =
  IO(StdIn.readLine())

def putStrLn(s: String): IO[Unit] = IO(println(s))

val readWhileNotHeisenberg: IO[Unit] = readLn.flatMap {
  case "Heisenberg" => IO.unit
  case notHeisenberg =>
    putStrLn(show"My name is not $notHeisenberg") *>
      putStrLn("Say it!") *>
      readWhileNotHeisenberg
}

val readName: IO[Unit] = {

  putStrLn("You know exactly who I am") *>
    putStrLn("Say my name") *>
    readWhileNotHeisenberg
}

val program = readName *> putStrLn("You're goddamn right")
