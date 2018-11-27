import cats.effect.IO
import scala.concurrent.Future

def future(): Future[Int]
val io: IO[Int]

val futureIO: IO[Int]     = IO.fromFuture(IO(future()))
val ioFuture: Future[Int] = io.unsafeToFuture()

<span class="fragment">//from monix-catnap 3.0.0-RC2
trait FutureLift[F[_], Future[_]] {
  def futureLift[A](fa: F[Future[A]]): F[A]
}</span>
