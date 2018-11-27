import java.util.UUID

import cats.effect.{ExitCode, IO, IOApp, Timer}
import cats.effect.concurrent.Semaphore

import scala.concurrent.duration._
import cats.implicits._

class MyService[F[_]](semaphore: Semaphore[F], timer: Timer[F]) {

  def limitedAction(input: String): F[Unit] = semaphore.withPermit {
    //simulates long-running task
    timer.sleep(5.seconds)
  }
}
<span class="fragment">
object MyServiceMain extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    for {
      semaphore <- Semaphore[IO](n = 5)
      service = new MyService(semaphore, timer)

      uuids: List[String] <- List.fill(10)(IO(UUID.randomUUID().toString)).sequence
      _                   <- uuids.parTraverse(service.limitedAction)
    } yield ExitCode.Success
}</span>
