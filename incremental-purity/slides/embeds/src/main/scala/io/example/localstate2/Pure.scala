package io.example.localstate2

import cats.data._
import cats.{Apply, FlatMap, Monad, Monoid}
import cats.implicits._
import cats.effect.{Console, ExitCode, IO, IOApp}
import cats.mtl.MonadState
import com.olegpy.meow.hierarchy._
import cats.mtl.instances.all._

import scala.concurrent.duration._

object Pure extends IOApp {

  def solveTasksPure(tasks: List[Task]): (Chain[String], FiniteDuration) = {

    def work[F[_]: HasSkillState: Monad: Log](task: Task): F[FiniteDuration] = {
      val knowsRequiredSkill: F[Boolean] = SkillState[F].get.map(_.contains(task.skill))

      knowsRequiredSkill.ifM(
        useKnownSkill[F](task),
        solveAndLearnSkill[F](task)
      )
    }

    type S[A] = WriterT[State[MyState, ?], Chain[String], A]

    //the compiler just can't derive this one automatically
    implicit val monad: Monad[S] = WriterT.catsDataMonadForWriterT
    implicit val console: Log[S] = s => WriterT.tell(Chain.one(s))

    tasks.foldMapM(work[S]).run.runA(MyState(Set.empty, Map.empty)).value
  }

  private def useKnownSkill[F[_]: Log: Apply](task: Task): F[FiniteDuration] = {
    val info = Log[F].putStrLn(s"I know how to do ${task.skill.name}! Adding only 1 second")

    info.as(1.second)
  }

  private def solveAndLearnSkill[F[_]: HasSkillState: FlatMap: Log](task: Task): F[FiniteDuration] = {
    val info = Log[F].putStrLn(s"Adding 10 secs for ${task.skill.name}")

    val learnSkill =
      AttemptState[F].get.map(_.get(task.skill)).flatMap {
        case Some(attemptsSoFar) if attemptsSoFar >= 4 => SkillState[F].modify(_ + task.skill)
        case _                                         => AttemptState[F].modify(_ |+| Map(task.skill -> 1))
      }

    info *> learnSkill.as(10.seconds)
  }

  case class MyState(skillsKnown: Set[Skill], attempts: Map[Skill, Int])
  type HasSkillState[F[_]] = MonadState[F, MyState]

  type SkillState[F[_]] = MonadState[F, Set[Skill]]
  def SkillState[F[_]](implicit F: SkillState[F]): SkillState[F] = F

  type AttemptState[F[_]] = MonadState[F, Map[Skill, Int]]
  def AttemptState[F[_]](implicit F: AttemptState[F]): AttemptState[F] = F

  implicit val durationMonoid: Monoid[FiniteDuration] = new Monoid[FiniteDuration] {
    override val empty: FiniteDuration                                         = 0.seconds
    override def combine(x: FiniteDuration, y: FiniteDuration): FiniteDuration = x + y
  }

  override def run(args: List[String]): IO[ExitCode] = {
    val (logs, time) = solveTasksPure(Imperative.tasks)

    logs.traverse_(Console.io.putStrLn(_)) *>
      IO(println(time))
        .as(ExitCode.Success)
  }

}
