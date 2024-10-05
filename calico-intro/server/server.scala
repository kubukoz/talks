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
import fs2.io.file.Files
import fs2.io.file.Path

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
                  case GET -> Root =>
                    builder.build {
                      _.drain.merge(
                        fs2.Stream.exec(IO.println("new file listener")) ++
                          Files[IO]
                            .readAll(Path(sys.env("HOME") + "/Downloads/shrek.txt"))
                            .through(fs2.text.utf8.decode)
                            .through(fs2.text.lines)
                            // .repeat
                            .map(WebSocketFrame.Text(_))
                            .onFinalizeCase { ec =>
                              IO.println(s"file listener DC: $ec")
                            }
                      )
                    }
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
