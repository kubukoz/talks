package example

import org.scalatest.AsyncWordSpec
import org.scalatest.Matchers
import org.http4s.client.Client
import org.http4s.HttpApp
import cats.effect.IO
import io.circe.Json
import org.http4s.Request
import org.http4s.Method
import org.http4s.Uri
import org.http4s.circe._
import org.http4s.implicits._
import cats.implicits._
import org.scalatest.OptionValues

class ServerRouteTests extends AsyncWordSpec with Matchers with IOTest with OptionValues {
  "Server" when {
    """POST /echo with body = {"foo": "bar"}""" should {
      "respond back" in {
        val remoteClient = Client.fromHttpApp(HttpApp.notFound[IO])

        val routes = Main.routes(remoteClient)

        val body = Json.obj("foo" -> Json.fromString("bar"))

        val request =
          Request[IO](method = Method.POST, uri = Uri.uri("/echo")).withEntity(body)

        val rawTest       = routes.run(request).value.flatMap(_.value.as[Json]).map(_ shouldBe body)
        val viaClientTest = Client.fromHttpApp(routes.orNotFound).expect[Json](request).map(_ shouldBe body)

        rawTest >> viaClientTest
      }.unsafeToFuture()
    }
  }
}
