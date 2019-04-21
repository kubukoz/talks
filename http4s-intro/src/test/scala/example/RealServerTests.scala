package example

import org.scalatest.AsyncWordSpec
import cats.effect.IO
import org.http4s.client.blaze.BlazeClientBuilder
import scala.concurrent.ExecutionContext
import org.scalatest.Matchers

class RealServerTests extends AsyncWordSpec with Matchers with IOTest {
  val blazeClient = BlazeClientBuilder[IO](ExecutionContext.global).resource

  "Server" when {
    "started" should {
      "respond to /hello" in {
        Main.server.use { server =>
          blazeClient.use { client =>
            client.expect[String](server.baseUri / "hello").map(_ shouldBe "Hello world!")
          }
        }.unsafeToFuture()
      }
    }
  }
}

trait IOTest {
  import cats.effect.ContextShift
  import cats.effect.Timer

  implicit val contextShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
  implicit val timer: Timer[IO]               = IO.timer(ExecutionContext.global)
}
