import scala.util.control.NonFatal

def withCloned[T](repoName: String)
                 (action: Repository => T): T = {
  try {
    cloneRepository()
    prepareDirectory()
    checkoutMaster()
    log.info("prepared")
    action(repoInfo(repoName))
  } catch {
    case NonFatal(ex) =>
      removeDirectory()
      throw ex
  }
}

trait Repository

def cloneRepository()
def prepareDirectory()
def checkoutMaster()
object log {
  def info(str: String) = ???
}
def repoInfo(name: String): Repository
def removeDirectory(): Unit
