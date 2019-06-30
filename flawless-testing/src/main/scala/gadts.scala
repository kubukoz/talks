package gadts

import java.time.Instant
import cats.effect.IO
import gadts.Payload.Text
import gadts.Payload.Zilch

final case class Message[P](id: Long, timestamp: Instant, payload: Payload[P])

object Message {
  def text(id: Long, timestamp: Instant, value: String): Message[String] = Message(id, timestamp, Payload.Text(value))
  def obj(id: Long, timestamp: Instant, value: Serializable): Message[Serializable] = Message(id, timestamp, Payload.Object(value))
  def ping(id: Long, timestamp: Instant): Message[Unit] = Message(id, timestamp, Payload.Zilch)
}

sealed trait Payload[P] extends Product with Serializable

object Payload {
  final case class Text(value: String) extends Payload[String]
  final case class Object(value: Serializable) extends Payload[Serializable]
  case object Zilch extends Payload[Unit]
}

object funs {

  def showText(message: Message[String]): IO[Unit] =
    message.payload match {
      case Text(value) =>
        IO {
          println(value)
        }
    }
}
