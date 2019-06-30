package puretests

import cats.implicits._
import java.time.ZonedDateTime
import cats.effect.IO
import cats.effect.concurrent.Ref
import cats.kernel.Eq
import cats.Show
import scala.concurrent.Future
import cats.effect.Resource
import flawless.Tests
import scala.util.Random
import _root_.cats.data.NonEmptyList
import cats.NonEmptyParallel

trait TimeProvider

object TimeProvider {
  def const(t: ZonedDateTime): TimeProvider = new TimeProvider {}
}

trait AppEvent

object AppEvent {
  case class FailedRent(time: ZonedDateTime) extends AppEvent
}

trait EventSender

object EventSender {
  def instance(send: AppEvent => IO[Unit]): EventSender = new EventSender {}
}

case class VideoId(value: Long)
trait RentResult
case class Failed(id: VideoId) extends RentResult

trait VideoService {
  def rent(id: VideoId): IO[RentResult]
}

object VideoService {

  def make(eventSender: EventSender, timeProvider: TimeProvider): VideoService = new VideoService {
    def rent(id: VideoId): IO[RentResult] = IO.pure(Failed(id))
  }
}

object puretests {

  import flawless.syntax._
  import flawless.syntax.idgaf._

  def testedFunction(i: Int): List[Int] = List.fill(i)(i)

  val bigTest = test("big test") {
    for {
      //given
      time <- IO(ZonedDateTime.now())
      timeProvider = TimeProvider.const(time)

      log <- Ref[IO].of(List.empty[AppEvent])
      eventSender = EventSender.instance(e => log.update(_ :+ e))
      videoService = VideoService.make(eventSender, timeProvider)

      //when
      rentResult <- videoService.rent(VideoId(1))
      logged     <- log.get
    } yield {
      val failedToRent = rentResult.shouldBe(Failed(VideoId(1)))
      val sentEvent = logged.shouldBe(List(AppEvent.FailedRent(time)))

      //then
      failedToRent |+| sentEvent
    }

  }

  val suite = tests(
    pureTest("hello") {
      testedFunction(1) shouldBe List(1)
    },
    pureTest("hello 2") {
      val result = testedFunction(2)

      result.size.shouldBe(2) |+|
        result.shouldBe(List(2, 2))
    },
    bigTest
  )

  object actors {
    case class ActorSystem() {
      def shutdown: Future[Unit] = Future.unit
    }

    val makeActorSystem: Resource[IO, ActorSystem] =
      Resource.make(IO(ActorSystem())) { as =>
        IO.fromFuture(IO(as.shutdown))
      }

    val suite = Tests.resource(makeActorSystem).use { as =>
      tests(
        test("system") {
          ???
        }
      )
    }
  }

  class future(implicit nep: NonEmptyParallel[IO, IO.Par]) {

    def fib(i: Int): Int = ???

    val suite = tests(
      test("flaky") {
        IO(Random.nextBoolean()).map(_ shouldBe true)
      },
      pureTest("obvious") {
        1 shouldBe 1
      }
    ).map(_.pure[NonEmptyList]) |+| Tests.parSequence {
      val results = List(1, 2, 3)

      NonEmptyList.of(1, 2, 3).map { i =>
        lazyTest(s"fib(${1000 * i})") {
          fib(i * 1000) shouldBe results(i)
        }
      }
    }
  }
}
