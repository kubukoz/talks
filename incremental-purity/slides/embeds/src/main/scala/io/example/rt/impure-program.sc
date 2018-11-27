def withCloned[T](repoName: String)
                 (action: Repository => T): T = {
  try {
    <span class="fragment">cloneRepository(repoName)</span>
    <span class="fragment">prepareDirectory()</span>
    <span class="fragment">checkoutMaster()</span>
    <span class="fragment">log.info("prepared")</span>
    <span class="fragment">action(repoInfo(repoName))</span>
  } finally {
    <span class="fragment">removeDirectory()</span>
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
