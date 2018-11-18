def withCloned[T](repoName: String)
                 (action: Repository => T): T = {
  try {
    cloneRepository(repoName)
    prepareDirectory()
    checkoutMaster()
    log.info("prepared")
    action(repoInfo(repoName))
  } finally {
    removeDirectory()
  }
}

object log {
  def info(str: String): Unit = ???
}

def cloneRepository(name: String): Unit
def prepareDirectory(): Unit
def checkoutMaster(): Unit
def repoInfo(name: String): Repository
def removeDirectory(): Unit

trait Repository
