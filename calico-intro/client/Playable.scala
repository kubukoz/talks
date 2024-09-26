import monocle.syntax.all.*
import cats.derived.*
import cats.kernel.Hash
import io.circe.Codec

enum Playable derives Hash, Codec.AsObject {
  case Play(noteId: Int, velocity: Int)
  case Rest

  def atVelocity(velocity: Int): Playable = mapPlay(_.focus(_.velocity).set(velocity))
  def +(semitones: Int): Playable = mapPlay(_.focus(_.noteId).modify(_ + semitones))
  def -(semitones: Int): Playable = mapPlay(_.focus(_.noteId).modify(_ - semitones))

  def mapPlay(f: Play => Play): Playable = this.focus(_.as[Play]).modify(f)

}

object Playable {

  // max velocity by default

  def play(noteId: Int): Playable.Play = Playable.Play(noteId, 0x7f)

  val C4 = play(60)
}
