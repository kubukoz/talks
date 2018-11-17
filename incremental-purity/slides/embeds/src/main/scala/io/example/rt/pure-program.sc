import cats.effect.IO
import cats.syntax.apply._

def withCloned[T](repoName: String)
                 (action: Repository => IO[T]): IO[T] = {

  val prepareRepo =
    cloneRepository *>
      prepareDirectory *>
      checkoutMaster *>
      log.info("prepared")

  val runInRepo = repoInfo(repoName).flatMap(action)

  val cleanup = removeDirectory

  (prepareRepo *> runInRepo).guarantee(cleanup)
}

trait Repository

def cloneRepository: IO[Unit]
def prepareDirectory: IO[Unit]
def checkoutMaster: IO[Unit]

object log {
  def info(str: String): IO[Unit] = IO(println(str))
}

def removeDirectory: IO[Unit]
def repoInfo(name: String): IO[Repository]
