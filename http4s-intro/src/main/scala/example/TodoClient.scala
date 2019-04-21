package example

import org.http4s.client.Client
import cats.effect.IO
import org.http4s.circe.CirceEntityDecoder._
import io.circe.generic.auto._
import org.http4s.Request
import org.http4s.Uri

case class Todo(id: Int, completed: Boolean)

class TodoClient(client: Client[IO]) {

  def getTodo(id: Int): IO[Todo] =
    client.expect[Todo](Request[IO](uri = Uri.uri("/todos") / id.toString))
}
