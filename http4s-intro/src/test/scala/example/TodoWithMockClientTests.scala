package example

import org.scalatest.AsyncWordSpec
import org.http4s.dsl.io._
import cats.effect.IO
import io.circe.generic.auto._
import org.http4s.client.Client
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.implicits._

import org.scalatest.Matchers
import org.http4s.HttpRoutes

class TodoWithMockClientTests extends AsyncWordSpec with Matchers {

  "Todo client" when {
    "server replies with OK" should {
      "find todo = 1" in {
        val mockServer = HttpRoutes
          .of[IO] {
            case GET -> Root / "todos" / IntVar(id) =>
              Ok(Todo(id, false))
          }
          .orNotFound

        val clientRaw  = Client.fromHttpApp(mockServer)
        val todoClient = new TodoClient(clientRaw)

        todoClient.getTodo(1).map(_ shouldBe Todo(1, false))
      }.unsafeToFuture()
    }
  }
}
