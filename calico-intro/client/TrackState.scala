import cats.effect.IO
import fs2.concurrent.Signal
import fs2.concurrent.SignallingRef

/// Abstraction for track state storage
/// which can be used to hide networking etc.
trait TrackState {
  def read: Signal[IO, List[List[Playable]]]
  def update(f: List[List[Playable]] => List[List[Playable]]): IO[Unit]
}

object TrackState {

  def fromSignallingRef(ref: SignallingRef[IO, List[List[Playable]]]): TrackState =
    new TrackState {
      def read: Signal[IO, List[List[Playable]]] = ref
      def update(f: List[List[Playable]] => List[List[Playable]]): IO[Unit] = ref.update(f)
    }

}
