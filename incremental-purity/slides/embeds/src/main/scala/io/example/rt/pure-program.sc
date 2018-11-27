import cats.effect.IO, cats.syntax.apply._

def withCloned[T](repoName: String)
                 (action: Repository => IO[T]): IO[T] = {

  val prepareRepo =
    <span class="fragment">cloneRepository(repoName) *>
      prepareDirectory *>
      checkoutMaster *>
      log.info("prepared")</span>

  val runInRepo = <span class="fragment">repoInfo(repoName).flatMap(action)</span>
  val cleanup = <span class="fragment">removeDirectory</span>

  (prepareRepo *> runInRepo).guarantee(cleanup)
}

trait Repository

def cloneRepository(repoName: String): IO[Unit] = IO.unit
def prepareDirectory: IO[Unit] = IO.unit
def checkoutMaster: IO[Unit] = IO.unit

object log {
  def info(str: String): IO[Unit] = IO(println(str))
}

def removeDirectory: IO[Unit] = IO.unit
def repoInfo(name: String): IO[Repository] = IO.never
