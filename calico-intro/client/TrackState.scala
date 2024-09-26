import cats.effect.Concurrent
import cats.effect.IO
import cats.effect.Temporal
import cats.effect.implicits.*
import cats.effect.kernel.Resource
import cats.effect.std.Hotswap
import cats.syntax.all.*
import fs2.concurrent.Signal
import fs2.concurrent.SignallingRef
import io.circe.Codec
import io.circe.syntax.*
import org.http4s.client.websocket.WSConnectionHighLevel
import org.http4s.client.websocket.WSFrame.Text

import scala.concurrent.duration.*
import scala.util.chaining.*

/// Abstraction for track state storage
/// which can be used to hide networking etc.
trait TrackState {
  def read: Signal[IO, List[List[Playable]]]
  def set(state: List[List[Playable]]): IO[Unit]
  def update(f: List[List[Playable]] => List[List[Playable]]): IO[Unit]
}

object TrackState {

  def fromSignallingRef(ref: SignallingRef[IO, List[List[Playable]]]): TrackState =
    new TrackState {
      def read: Signal[IO, List[List[Playable]]] = ref
      def set(state: List[List[Playable]]): IO[Unit] = ref.set(state)
      def update(f: List[List[Playable]] => List[List[Playable]]): IO[Unit] = ref.update(f)
    }

  def remote(channel: DataChannel[IO], init: List[List[Playable]]): Resource[IO, TrackState] =
    SignallingRef[IO]
      .of(init)
      .toResource
      .flatTap { ref =>
        // consume external changes
        channel
          .receive
          .evalMap {
            case Message.Set(state) => ref.set(state)
            case Message.Get        => ref.get.map(Message.Set(_)).flatMap(channel.send)
          }
          .compile
          .drain
          .background
      }
      .map { ref =>
        new {
          def read: Signal[IO, List[List[Playable]]] = ref
          def set(state: List[List[Playable]]): IO[Unit] =
            ref.set(state) *>
              channel.send(Message.Set(state))

          def update(f: List[List[Playable]] => List[List[Playable]])
            : IO[Unit] = ref.updateAndGet(f).flatMap { newState =>
            channel.send(Message.Set(newState))
          }
        }

      }

}

enum Message derives Codec.AsObject {
  case Set(state: List[List[Playable]])
  case Get
}

trait DataChannel[F[_]] {
  def send(msg: Message): F[Unit]
  def receive: fs2.Stream[F, Message]
  def state: Signal[F, DataChannel.State]
}

object DataChannel {

  enum State {
    case Connected
    case Connecting
  }

  def fromWebSocket[F[_]: Concurrent: cats.effect.std.Console](ws: WSConnectionHighLevel[F])
    : DataChannel[F] =
    new {
      def send(msg: Message): F[Unit] = ws.sendText(msg.asJson.noSpaces)
      def receive: fs2.Stream[F, Message] = ws
        .receiveStream
        .collect { case Text(data, _) => data }
        .evalMap(io.circe.parser.decode[Message](_).liftTo[F])
        .onError { case e =>
          fs2.Stream.eval_(cats.effect.std.Console[F].println(s"WebSocket error: $e"))
        }
        .onFinalizeCase { ec =>
          cats.effect.std.Console[F].print(s"WebSocket closed: $ec")
        }

      // we deal with proper state on another level
      def state: Signal[F, State] = Signal.constant(State.Connected)
    }

  // This is a bit messy, but so is the actual problem.
  // The problem we have here is that WebSocket connections can fail, and this happens outside of our direct control.
  // A close can be recognized by the fact that the receive stream completes. In our case here, this means the DataChannel's receive stream ends.
  // This event triggers a reconnect attempt, and any attempts to send will fail (with a retry) until the connection is re-established.
  // The implementation is imperfect, and requires the connection to succeed the first time (at initialization) before progress can be made on the downstream
  // (e.g. rendering UI).
  // A proper implementation would encapsulate the reconnection logic and allow this to initialize the connection in the background,
  // while also queueing up any messages that are sent before the connection is established (in a controlled manner). Exercise for the reader ;)
  def fromFallible[F[_]: Temporal: cats.effect.std.Console](
    make: Resource[F, DataChannel[F]]
  ): Resource[F, DataChannel[F]] =
    (Hotswap(make).map(_._1), SignallingRef[F].of(State.Connected).toResource)
      .mapN { (swap, stateRef) =>
        new {
          def send(msg: Message): F[Unit] = swap
            .get
            .use(
              _.liftTo[F](new Exception("channel not available!")).flatMap(_.send(msg))
            )
            .retrying("send")

          def receive: fs2.Stream[F, Message] = fs2
            .Stream
            .resource(swap.get)
            .flatMap {
              _.liftTo[F](new Exception("channel not available!"))
                .pipe(fs2.Stream.eval(_))
                .flatMap(_.receive)
            }
            .onFinalizeCase { ec =>
              cats.effect.std.Console[F].println(s"DataChannel closed: $ec") *>
                swap.clear *>
                stateRef.set(State.Connecting) *>
                swap
                  .swap(make)
                  .retrying("reconnect")
                  .void *>
                stateRef.set(State.Connected)
            }
            .attempt
            .repeat
            // do NOT drain this ;)
            .collect { case Right(msg) => msg }

          def state: Signal[F, State] = stateRef

          extension [A](fa: F[A]) {
            def retrying(tag: String): F[A] = fa
              .onError { case e =>
                cats.effect.std.Console[F].errorln(s"Exception while retrying $tag")
              }
              .handleErrorWith(_ => fa.retrying(tag).delayBy(1.second))

          }
        }
      }

}
