import monocle.syntax.all.*
import cats.derived.*
import cats.kernel.Hash
import io.circe.Codec

enum Playable derives Hash, Codec.AsObject {
  case Play(noteId: Int, velocity: Int)
  case Rest

  def atVelocity(velocity: Int): Playable = mapPlay(_.focus(_.velocity).replace(velocity))
  def +(semitones: Int): Playable = mapPlay(_.focus(_.noteId).modify(_ + semitones))
  def -(semitones: Int): Playable = mapPlay(_.focus(_.noteId).modify(_ - semitones))

  def mapPlay(f: Play => Play): Playable = this.focus(_.as[Play]).modify(f)

  def ifRest[A](f: => A): Option[A] =
    this match {
      case Rest => Some(f)
      case _    => None
    }

  def ifPlay[A](f: Play => A): Option[A] =
    this match {
      case play: Play => Some(f(play))
      case _          => None
    }

}

object Playable {

  // max velocity by default

  def play(noteId: Int): Playable.Play = Playable.Play(noteId, 0x7f)

  val C4 = play(60)
}
