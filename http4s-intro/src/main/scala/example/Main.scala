package example

import cats.effect.{ExitCode, IO}
import cats.effect.IOApp
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.HttpRoutes
import org.http4s.implicits._
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import scala.concurrent.ExecutionContext
import org.http4s.Uri
import cats.effect.Resource
import org.http4s.server.Server
import org.http4s.Request
import org.http4s.dsl.io._
import fs2.concurrent.Queue
import org.http4s.server.websocket.WebSocketBuilder
import org.http4s.websocket.WebSocketFrame

object Main extends IOApp {

  def routes(client: Client[IO]): HttpRoutes[IO] = {
    val request = Request[IO](uri = Uri.uri("https://http4s.org"))

    HttpRoutes.of[IO] {
      case GET -> Root / "hello" => Ok("Hello world!")

      case request @ POST -> Root / "echo" => Ok(request.body)

      case GET -> Root / "remote" =>
        val remoteCall: IO[String] =
          client.expect[String](request)

        for {
          result   <- remoteCall
          response <- Ok(result)
        } yield response

      case GET -> Root / "remote" / "stream" =>
        val response = client.stream(request).flatMap(_.body)

        Ok(response)

      case GET -> Root / "ws" / "echo" =>
        Queue.bounded[IO, WebSocketFrame](100).flatMap { q =>
          WebSocketBuilder[IO].build(
            send = q.dequeue,
            receive = q.enqueue
          )
        }
    }
  }

  val server: Resource[IO, Server[IO]] =
    BlazeClientBuilder[IO](ExecutionContext.global).resource.flatMap {
      client =>
        BlazeServerBuilder[IO]
          .withHttpApp(routes(client).orNotFound)
          .bindHttp(port = 8080, host = "0.0.0.0")
          .resource
    }

  def run(args: List[String]): IO[ExitCode] =
    server.use(_ => IO.never)
}

import cats.effect.Bracket

object BaseUrl {

  def apply[F[_]](base: Uri)(
    client: Client[F]
  )(implicit F: Bracket[F, Throwable]): Client[F] = {
    Client.apply[F] { req =>
      client.run(req.withUri(base.resolve(req.uri)))
    }
  }
}
