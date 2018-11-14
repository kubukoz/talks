package io.example.localstate2

import cats.{Apply, FlatMap, Monad, Monoid}
import cats.effect.concurrent.Ref
import cats.implicits._
import cats.effect.{Console, ExitCode, IO, IOApp}
import cats.mtl.MonadState
import com.olegpy.meow.effects._
import com.olegpy.meow.hierarchy._
import cats.mtl.instances.all._

import scala.concurrent.duration._

object Pure extends IOApp {

  def solveTasksPure(tasks: List[Task]): IO[FiniteDuration] = {

    def work[F[_]: HasSkillState: Monad: Console](task: Task): F[FiniteDuration] = {
      val knowsRequiredSkill: F[Boolean] = SkillState[F].get.map(_.contains(task.skill))

      knowsRequiredSkill.ifM(
        useKnownSkill[F](task),
        solveAndLearnSkill[F](task)
      )
    }

    implicit val console: Console[IO] = Console.io

    Ref[IO]
      .of(MyState(Set.empty, Map.empty))
      .flatMap {
        _.runState { implicit RS =>
          tasks.foldMapM(work[IO])
        }
      }
  }

  private def useKnownSkill[F[_]: Console: Apply](task: Task): F[FiniteDuration] = {
    val info = Console[F].putStrLn(s"I know how to do ${task.skill.name}! Adding only 1 second")

    info.as(1.second)
  }

  private def solveAndLearnSkill[F[_]: HasSkillState: FlatMap: Console](task: Task): F[FiniteDuration] = {
    val info = Console[F].putStrLn(s"Adding 10 secs for ${task.skill.name}")

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
    solveTasksPure(Imperative.tasks)
      .flatMap(duration => IO(println(duration)))
      .as(ExitCode.Success)
  }

}
