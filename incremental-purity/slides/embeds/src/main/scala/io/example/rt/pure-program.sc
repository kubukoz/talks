import cats.effect.IO, cats.syntax.apply._

def withCloned[T](repoName: String)
                 (action: Repository => IO[T]): IO[T] = {

  val prepareRepo =
    cloneRepository(repoName) *>
      prepareDirectory *>
      checkoutMaster *>
      log.info("prepared")

  val runInRepo = repoInfo(repoName).flatMap(action)
  val cleanup = removeDirectory

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
