package first

import java.time.Instant

sealed trait Message extends Product with Serializable {

  def ident: Long = this match {
    case TextMessage(i, _, _)   => i
    case ObjectMessage(i, _, _) => i
    case Ping(i, _)             => i
  }

  def ts: Instant = this match {
    case TextMessage(_, ts, _)   => ts
    case ObjectMessage(_, ts, _) => ts
    case Ping(_, ts)             => ts
  }

}

final case class TextMessage(id: Long, timestamp: Instant, payload: String) extends Message
final case class ObjectMessage(id: Long, timestamp: Instant, payload: Serializable) extends Message
final case class Ping(id: Long, timestamp: Instant) extends Message
