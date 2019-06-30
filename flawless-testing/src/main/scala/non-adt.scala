package nonadt
import java.time.Instant

trait Message {
  def id: Long
  def timestamp: Instant
}

final case class TextMessage(id: Long, timestamp: Instant, payload: String) extends Message
final case class ObjectMessage(id: Long, timestamp: Instant, payload: Serializable) extends Message
final case class Ping(id: Long, timestamp: Instant) extends Message
