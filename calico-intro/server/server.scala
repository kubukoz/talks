//> using dep org.http4s::http4s-ember-server::0.23.28
//> using dep org.http4s::http4s-dsl::0.23.28
//> using option -no-indent
import language.experimental.namedTuples
import cats.effect.IO
import cats.effect.IOApp
import cats.effect.std.UUIDGen
import cats.syntax.all.*
import fs2.concurrent.Topic
import org.http4s.HttpApp
import org.http4s.dsl.io.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.websocket.WebSocketFrame

import java.util.UUID
import scala.concurrent.duration.*
import fs2.concurrent.SignallingRef
import org.http4s.server.websocket.WebSocketBuilder2
import org.http4s.Response

import com.comcast.ip4s.*
import org.http4s.HttpRoutes

object ws extends IOApp.Simple {

  def run: IO[Unit] = {
    val srv =
      for {
        messages <- Topic[IO, (sender: UUID, frame: WebSocketFrame)].toResource
        _ <-
          EmberServerBuilder
            .default[IO]
            .withShutdownTimeout(Duration.Zero)
            .withHost(host"0.0.0.0")
            .withPort(port"8080")
            .withHttpWebSocketApp { builder =>
              val respond = build(builder, messages)

              HttpRoutes
                .of[IO] {
                  case GET -> Root / "ws" / "leader" => respond(Peer.Leader)
                  case GET -> Root / "ws"            => respond(Peer.Follower)
                }
                .orNotFound
            }
            .build
        _ <-
          messages
            .subscribers
            .changes
            .debug("subscribers: " + _)
            .compile
            .drain
            .background
      } yield ()
    srv.useForever
  }

  def build(
    builder: WebSocketBuilder2[IO],
    messages: Topic[IO, (sender: UUID, frame: WebSocketFrame)],
  )(
    peer: Peer
  ): IO[Response[IO]] =
    IO.println(s"peer connected: $peer") *>
      builder.build { input =>
        fs2
          .Stream
          .eval(UUIDGen[IO].randomUUID.debug("connected"))
          .flatMap { myId =>
            input
              .debug(s"send ($myId): " + _)
              .tupleLeft(myId)
              .through(messages.publish)
              .mergeHaltBoth(
                messages
                  .subscribeUnbounded
                  .filterNot(_.sender == myId)
                  .debug(s"receive ($myId): " + _)
                  .map(_.frame)
              )
              .onFinalize(IO.pure(myId).debug("disconnected").void)
          }
      }

}

enum Peer {
  case Leader
  case Follower
}