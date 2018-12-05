package io.example.samples
import java.util.concurrent.Executors

import cats.syntax.functor._
import cats.effect._

import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService}

class ContextShiftUsage[F[_]: ContextShift: Sync](blockingEc: ExecutionContext) {

  private val blockingBlock = Sync[F].delay(Thread.sleep(1000))

  //returns to CS's default thread pool after the evaluated block
  val blockingAction: F[Unit] = ContextShift[F].evalOn(blockingEc)(blockingBlock)
}








//keep scrolling

object Main2 extends IOApp {

  val unboundedEc: Resource[IO, ExecutionContextExecutorService] =
    Resource.make(IO(ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())))(ec => IO(ec.shutdown()))

  override def run(args: List[String]): IO[ExitCode] = {

    unboundedEc.use { blockingEc =>
      val app = new ContextShiftUsage[IO](blockingEc)

      app.blockingAction
        .as(ExitCode.Success)
    }
  }
}
