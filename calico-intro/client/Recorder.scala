import cats.effect.IO
import cats.effect.implicits.*
import cats.effect.kernel.Ref
import cats.syntax.all.*
import fs2.concurrent.SignallingRef

object Recorder {

  def run(
    instrument: Instrument,
    currentNoteRef: Ref[IO, Int],
    recordingRef: Ref[IO, Boolean],
    recordingTrackRef: Ref[IO, Int],
    trackState: TrackState,
    octavesRef: SignallingRef[IO, Int],
  ): IO[Unit] =
    fs2
      .Stream
      .emits("awsedftgyhujkolp".toList.zipWithIndex)
      .map { (key, i) =>
        KeyStatus
          .forKey(key.show)
          .changes
          .tupleRight(i)
          .zipWith(fs2.Stream.repeatEval(octavesRef.get))(_ :* _)
      }
      .parJoinUnbounded
      .parEvalMapUnbounded { (isOn, i, octaves) =>
        val noteToPlay = i + 60 + (octaves * 12)
        val velocity = 100

        val play =
          if isOn then instrument.play(noteToPlay, velocity)
          else
            instrument.stop(noteToPlay)

        val register = (currentNoteRef.get, recordingRef.get, recordingTrackRef.get).flatMapN {
          case (currentNote, true, track) =>
            trackState.updateAtAndGet(track, currentNote) { _ =>
              Playable.Play(noteToPlay, velocity)
            }
          case _ => IO.unit
        }

        play *> register.whenA(isOn)
      }
      .concurrently(
        KeyStatus
          .forKey("z")
          .changes
          .filter(identity)
          .foreach(_ => octavesRef.update(_ - 1))
      )
      .concurrently(
        KeyStatus
          .forKey("x")
          .changes
          .filter(identity)
          .foreach(_ => octavesRef.update(_ + 1))
      )
      .compile
      .drain

}
