import fs2.Stream
import cats.effect._
import cats.implicits._
import io.chrisdavenport.log4cats.Logger
import scala.concurrent.duration._

trait OrderStorage[F[_]] {
  def countOrders: F[Int]
}

object OrderStorage {
  def apply[F[_]](implicit F: OrderStorage[F]): OrderStorage[F] = F
}

class OrderStream[F[_]: Timer: OrderStorage: Logger] {
  val log = Logger[F]
  val storage = OrderStorage[F]

val schedule = Stream.sleep[F](5.seconds).repeat

val orderCountScheduledJob: Stream[F, Unit] =
  Stream
    .repeatEval(storage.countOrders)
    .evalMap(count => log.info(s"Current order count: $count"))
    .zipLeft(schedule)
}
