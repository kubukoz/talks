package reimagined

import java.time.Instant

final case class Message(id: Long, timestamp: Instant, payload: Payload)

object Message {
  def text(id: Long, timestamp: Instant, value: String): Message = Message(id, timestamp, Payload.Text(value))
  def obj(id: Long, timestamp: Instant, value: Serializable): Message = Message(id, timestamp, Payload.Object(value))
  def ping(id: Long, timestamp: Instant): Message = Message(id, timestamp, Payload.Zilch)
}

sealed trait Payload extends Product with Serializable

object Payload {
  final case class Text(value: String) extends Payload
  final case class Object(value: Serializable) extends Payload
  case object Zilch extends Payload
}
