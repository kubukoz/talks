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

object ws extends IOApp.Simple {

  def run: IO[Unit] = {
    val srv =
      for {
        messages <- Topic[IO, (sender: UUID, frame: WebSocketFrame)].toResource
        _ <-
          EmberServerBuilder
            .default[IO]
            .withShutdownTimeout(Duration.Zero)
            .withHttpWebSocketApp { builder =>
              val respond = build(builder, messages)

              HttpApp.apply[IO] {
                case GET -> Root / "leader" => respond(Peer.Leader)
                case _                      => respond(Peer.Follower)
              }
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
              .tupleLeft(myId)
              .debug()
              .through(messages.publish)
              .mergeHaltBoth(
                messages
                  .subscribeUnbounded
                  .filterNot(_.sender == myId)
                  .map(_.frame)
              )
              .debug()
              .onFinalize(IO.pure(myId).debug("disconnected").void)
          }
      }

}

enum Peer {
  case Leader
  case Follower
}
