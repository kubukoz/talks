package play

//stubs
trait Config 

trait ApplicationLifecycle {
  def addStopHook(hook: => Unit): Unit
}

object Imports {
  object db {
    def close(): Unit = ()
  }
  def newHikariPool(config: Config): db.type = db
}
import  Imports._

//actual code
class DataSource @Inject()(
  config: Config,
  lifecycle: ApplicationLifecycle
) {
  val db = newHikariPool(config)

  lifecycle.addStopHook {
    db.close()
  }
}
