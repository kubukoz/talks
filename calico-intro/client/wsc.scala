import calico.*
import calico.html.io.*
import calico.html.io.given
import cats.ApplicativeThrow
import cats.FlatMap
import cats.Functor
import cats.effect.IO
import cats.effect.kernel.Ref
import cats.effect.kernel.Resource
import cats.effect.kernel.Resource.ExitCase
import cats.effect.std.Console as Stdio
import cats.effect.std.Queue
import cats.syntax.all.*
import fs2.Chunk
import fs2.concurrent.SignallingRef
import fs2.dom.HtmlElement
import fs2.dom.ext.RTCDataChannel
import fs2.dom.ext.RTCPeerConnection
import io.circe.Codec
import io.circe.Decoder
import io.circe.Json
import io.circe.scalajs as cjs
import io.circe.syntax.*
import org.http4s.client.websocket.WSConnectionHighLevel
import org.http4s.client.websocket.WSFrame.Binary
import org.http4s.client.websocket.WSFrame.Text
import org.http4s.client.websocket.WSRequest
import org.http4s.dom.WebSocketClient
import org.http4s.implicits.*
import org.scalajs.dom

import scala.scalajs.js.JSON

def codecViaJsAny[A <: scalajs.js.Any]: Codec[A] = Codec.from(
  Decoder[Json].map(cjs.convertJsonToJs(_)).map(_.asInstanceOf[A]),
  // for some reason, this emits empty objects for Candidate lol
  // cjs.convertJsToJson(_).toTry.get,
  v => io.circe.parser.parse(JSON.stringify(v)).toTry.get,
)

given Codec[dom.RTCSessionDescription] = codecViaJsAny

given Codec[dom.RTCIceCandidate] = codecViaJsAny

enum Message derives Codec.AsObject {
  case Offer(offer: dom.RTCSessionDescription)
  case Answer(answer: dom.RTCSessionDescription)
  case Candidate(candidate: dom.RTCIceCandidate)

  override def toString(): String =
    this match {
      case Answer(answer) => s"Answer(${JSON.stringify(answer)})"
      case Offer(offer)   => s"Offer(${JSON.stringify(offer)})"
      case Candidate(c)   => s"Candidate(${JSON.stringify(c)})"
    }

}

enum Peer {
  case Leader
  case Follower

  def invert: Peer =
    this match {
      case Leader   => Follower
      case Follower => Leader
    }

}

object wsc extends IOWebApp {

  def render: Resource[IO, HtmlElement[IO]] =
    for {
      listenerRef <- IO.ref((s: String) => IO.println("default listenerRef: " + s)).toResource
      messages <- SignallingRef[IO].of(List.empty[String]).toResource

      peer <-
        window
          .location
          .search
          .get
          .map {
            case "?leader" => Peer.Leader
            case _         => Peer.Follower
          }
          .toResource

      _ <-
        setupConnection(
          listenerRef,
          onReceive = msg => messages.update(_.prepended(s"From ${peer.invert}: " + msg)),
          peer = peer,
        ).useForever.background

      messageRef <- SignallingRef[IO].of("").toResource
      view <- div(
        form(
          input.withSelf { self =>
            (
              placeholder := "Your message",
              value <-- messageRef,
              onChange --> (_.foreach(_ => self.value.get.flatMap(messageRef.set))),
            )
          },
          button(`type` := "submit", "send"),
          onSubmit --> (_.foreach(e =>
            e.preventDefault *> listenerRef.get.ap(messageRef.get).flatten *> messageRef.set("")
          )),
        ),
        ul(
          children <-- messages
            .map(_.toList)
            .map(_.map(li(_)))
        ),
      )

    } yield view

  def setupConnection(
    listenerRef: Ref[IO, String => IO[Unit]],
    onReceive: String => IO[Unit],
    peer: Peer,
  ): Resource[IO, Unit] =
    peer match {
      case Peer.Leader =>
        for {
          ws <- WebSocketClient[IO].connectHighLevel(WSRequest(uri"ws://localhost:8080/leader"))
          signalling = RTCSignalling.fromWebSocket[IO, Message](ws).logged(peer.toString)

          receiveChannel <- IO.deferred[RTCDataChannel[IO]].toResource

          // don't look, this is supposed to be a map and hidden behind a decent abstraction too
          // but my PTO is ending so I don't have much time to make it nice.
          peerConnectionPool <-
            Resource.makeCase {
              SignallingRef[IO].of(
                Option.empty[((RTCPeerConnection[IO], RTCDataChannel[IO]), ExitCase => IO[Unit])]
              )
            }((ref, exit) => ref.get.flatMap(_.traverse_(_._2(exit))))

          _ <- peerConnectionPool.discrete.debug("peerCP: " + _).compile.drain.background
          _ <-
            listenerRef.update { old => msg =>
              old(msg) *>
                RTCDataChannel
                  .fromDeferred(receiveChannel)
                  .send(msg)
            }.toResource

          _ <-
            signalling
              .receiveStream
              .foreach {
                case Message.Offer(offer) =>
                  for {
                    ((peerConnection, sendChannel), cleanupPeer) <- IO.uncancelable { poll =>
                      val resource = RTCPeerConnection[IO](config = none).mproduct {

                        _.createDataChannel(
                          "chat",
                          new dom.RTCDataChannelInit {},
                        )
                      }

                      poll(resource.allocatedCase).flatTap(v => peerConnectionPool.set(v.some))
                    }

                    _ <- peerConnection.onDataChannel(
                      IO.println("leader receive channel present") *> receiveChannel
                        .complete(_)
                        .void
                    )
                    _ <- peerConnection
                      .onIceConnectionStateChange(e =>
                        IO(dom.console.log("leader ice state change:", e))
                      )
                    _ <- sendChannel.onOpen {
                      IO.println("Leader data channel is open")
                    }
                    _ <- sendChannel.onMessage { event =>
                      IO(dom.console.log("Leader data channel received message", event)) *>
                        onReceive(event.data.toString())
                    }
                    _ <- peerConnection.onIceCandidate { event =>
                      // so much for null safety in Scala
                      IO.whenA(event.candidate != null) {
                        signalling.send(Message.Candidate(event.candidate))
                      }
                    }

                    _ <- peerConnection.setRemoteDescription(offer)
                    answer <- peerConnection.createAnswer
                    _ <- peerConnection.setLocalDescription(answer)
                    _ <- signalling.send(Message.Answer(answer))
                  } yield ()

                case Message.Answer(_) => IO.println("Ignoring answer message as we're the leader")
                case Message.Candidate(candidate) =>
                  peerConnectionPool.get.flatMap {
                    case None => IO.println("WARNING: received candidate with no peer in pool")
                    case Some(((peerConnection, _), _)) => peerConnection.addIceCandidate(candidate)
                  }
              }
              .compile
              .drain
              .background
        } yield ()

      case Peer.Follower =>
        // create a connection, send an offer, listen for answers.
        for {
          ws <- WebSocketClient[IO].connectHighLevel(WSRequest(uri"ws://localhost:8080/leader"))
          signalling = RTCSignalling.fromWebSocket[IO, Message](ws).logged(peer.toString)
          peerConnection <- RTCPeerConnection[IO](config = none)

          sendChannel <- peerConnection.createDataChannel("chat", new dom.RTCDataChannelInit {})
          receiveChannel <- IO.deferred[RTCDataChannel[IO]].toResource
          _ <-
            peerConnection
              .onDataChannel(
                IO.println("follower receive channel present") *> receiveChannel.complete(_).void
              )
              .toResource

          _ <-
            peerConnection
              .onIceConnectionStateChange(e => IO(dom.console.log("follower ice state change:", e)))
              .toResource
          _ <-
            listenerRef.update { old => msg =>
              old(msg) *>
                RTCDataChannel
                  .fromDeferred(receiveChannel)
                  .send(msg)
            }.toResource
          _ <-
            sendChannel.onOpen {
              IO.println("Follower data channel is open")
            }.toResource
          _ <-
            sendChannel.onMessage { event =>
              IO(dom.console.log("Follower data channel received message", event)) *>
                onReceive(event.data.toString())
            }.toResource
          _ <-
            peerConnection.onIceCandidate { event =>
              // so much for null safety in Scala
              IO.whenA(event.candidate != null) {
                signalling.send(Message.Candidate(event.candidate))
              }
            }.toResource

          _ <-
            signalling
              .receiveStream
              .foreach {
                case Message.Offer(_) => IO.println("Ignoring offer message as we're a follower")
                case Message.Answer(answer)       => peerConnection.setRemoteDescription(answer)
                case Message.Candidate(candidate) => peerConnection.addIceCandidate(candidate)
              }
              .compile
              .drain
              .background
          _ <- IO.println("Follower creating offer").toResource
          offer <- peerConnection.createOffer.toResource
          _ <- peerConnection.setLocalDescription(offer).toResource
          _ <- signalling.send(Message.Offer(offer)).toResource
        } yield ()
    }

}

trait RTCSignalling[F[_], Msg] {
  def send(msg: Msg): F[Unit]
  def receiveStream: fs2.Stream[F, Msg]
}

object RTCSignalling {

  def fromWebSocket[F[_]: ApplicativeThrow, A: Codec](ws: WSConnectionHighLevel[F])
    : RTCSignalling[F, A] =
    new RTCSignalling[F, A] {
      def send(msg: A): F[Unit] = ws.sendText(msg.asJson.noSpaces)
      def receiveStream: fs2.Stream[F, A] = ws
        .receiveStream
        .evalMap {
          case Text(data, _) => io.circe.parser.decode[A](data).liftTo[F]
          case Binary(_, _)  => new Throwable("Binary messages not supported").raiseError
        }
    }

  def fromQueues[F[_]: Functor, Msg](from: Queue[F, Chunk[Msg]], to: Queue[F, Chunk[Msg]])
    : RTCSignalling[F, Msg] =
    new RTCSignalling[F, Msg] {
      def send(msg: Msg): F[Unit] = to.offer(Chunk.singleton(msg))
      def receiveStream: fs2.Stream[F, Msg] = fs2.Stream.fromQueueUnterminatedChunk(from)
    }

  def logged[F[_]: Stdio: FlatMap, Msg](
    label: String,
    underlying: RTCSignalling[F, Msg],
  ): RTCSignalling[F, Msg] =
    new RTCSignalling[F, Msg] {
      def send(msg: Msg): F[Unit] =
        Stdio[F].println(s"$label sending: $msg") *>
          underlying.send(msg)

      def receiveStream: fs2.Stream[F, Msg] = underlying
        .receiveStream
        .debug(s"$label receiving: " + _)
    }

  extension [F[_]: Stdio: FlatMap, Msg](self: RTCSignalling[F, Msg]) {
    def logged(label: String): RTCSignalling[F, Msg] = RTCSignalling.logged(label, self)
  }

}
