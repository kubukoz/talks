package io.example.localstate2

import cats.Monad
import cats.data.StateT
import cats.implicits._
import cats.effect.{Console, ExitCode, IO, IOApp, SyncConsole}
import cats.mtl.MonadState

import scala.concurrent.duration._

object Pure extends IOApp {
  import com.olegpy.meow.hierarchy._
  import cats.mtl.instances.all._

  def solveTasksPure(tasks: List[Task]): IO[FiniteDuration] = {

    def work[F[_]: HasSkillState: Monad: Console](task: Task): F[Unit] = {
      SkillState[F].get.flatMap { skills =>
        if (skills.contains(task.skill)) {
          val info = Console[F].putStrLn(s"I know how to do ${task.skill.name}! Adding only 1 second")

          info *> TimeState[F].modify(_ + 1.second)
        } else {
          val learnSkill =
            AttemptState[F].get.map(_.get(task.skill)).flatMap {
              case Some(attemptsSoFar) if attemptsSoFar >= 4 => SkillState[F].modify(_ + task.skill)
              case _                                         => AttemptState[F].modify(_ |+| Map(task.skill -> 1))
            }

          val info = Console[F].putStrLn(s"Adding 10 secs for ${task.skill.name}")

          info *> learnSkill *> TimeState[F].modify(_ + 10.seconds)
        }
      }
    }

    implicit val console = new SyncConsole[StateT[IO, SkillState, ?]]

    tasks
      .traverse(work[StateT[IO, SkillState, ?]])
      .runS(SkillState(0.seconds, Set.empty, Map.empty))
      .value
      .map(_.timeSoFar)
  }

  case class SkillState(timeSoFar: FiniteDuration, skillsKnown: Set[Skill], attempts: Map[Skill, Int])
  type HasSkillState[F[_]] = MonadState[F, SkillState]

  type TimeState[F[_]] = MonadState[F, FiniteDuration]
  def TimeState[F[_]](implicit F: TimeState[F]): TimeState[F] = F

  type SkillState[F[_]] = MonadState[F, Set[Skill]]
  def SkillState[F[_]](implicit F: SkillState[F]): SkillState[F] = F

  type AttemptState[F[_]] = MonadState[F, Map[Skill, Int]]
  def AttemptState[F[_]](implicit F: AttemptState[F]): AttemptState[F] = F

  override def run(args: List[String]): IO[ExitCode] = {
    solveTasksPure(Imperative.tasks)
      .flatMap(duration => IO(println(duration)))
      .as(ExitCode.Success)
  }
}
