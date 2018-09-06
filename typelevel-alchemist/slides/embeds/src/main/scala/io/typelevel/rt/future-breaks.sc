import scala.concurrent.Future

val f: Future[Unit] = launchNukes()

//left launches once, right launches twice
(f, f) <-/-> (launchNukes(), launchNukes())