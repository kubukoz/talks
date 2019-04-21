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
import org.http4s.dsl.io._

object Main extends IOApp {

  import org.http4s.Request

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
    }
  }

  val server: Resource[IO, Server[IO]] =
    BlazeClientBuilder[IO](ExecutionContext.global).resource.flatMap { client =>
      BlazeServerBuilder[IO]
        .withHttpApp(routes(client).orNotFound)
        .bindHttp(port = 8080)
        .resource
    }

  def run(args: List[String]): IO[ExitCode] =
    server.use(_ => IO.never)
}
