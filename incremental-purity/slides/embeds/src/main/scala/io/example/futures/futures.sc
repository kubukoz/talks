import cats.effect.IO
import scala.concurrent.Future

def future(): Future[Int]
val io: IO[Int]

val futureIO: IO[Int]     = IO.fromFuture(IO(future()))
val ioFuture: Future[Int] = io.unsafeToFuture()
