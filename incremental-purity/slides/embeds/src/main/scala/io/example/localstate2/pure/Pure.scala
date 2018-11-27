package io.example.localstate2.pure

import cats.data._
import cats.{FlatMap, Functor, Monoid}
import cats.implicits._
import cats.effect._
import cats.mtl.MonadState
import cats.mtl.instances.all._
import io.example.localstate2.{Log, Skill, Task}

import scala.concurrent.duration._

object Pure extends IOApp {

  def solveTasksPure(tasks: List[Task]): (Chain[String], FiniteDuration) = {
    type S[A] = WriterT[State[MyState, ?], Chain[String], A]

    implicit val console: Log[S] = s => WriterT.tell(Chain.one(s))

    val startState = MyState(Set.empty, Map.empty)

    tasks.foldMapM(work[S]).run.runA(startState).value
  }

  import com.olegpy.meow.hierarchy._

  private def work[F[_]: HasSkillState: FlatMap: Log](task: Task): F[FiniteDuration] = {
    val knowsRequiredSkill: F[Boolean] = SkillState[F].get.map(_.contains(task.skill))

    knowsRequiredSkill.ifM(
      useKnownSkill[F](task),
      solveAndLearnSkill[F](task)
    )
  }

  private def useKnownSkill[F[_]: Log: Functor](task: Task): F[FiniteDuration] = {
    val info = Log[F].putStrLn(s"I know how to do ${task.skill.name}! Adding only 1 second")

    info.as(1.second)
  }

  private def solveAndLearnSkill[F[_]: HasSkillState: FlatMap: Log](task: Task): F[FiniteDuration] = {
    val info = Log[F].putStrLn(s"Adding 10 secs for ${task.skill.name}")

    val attemptCount          = AttemptState[F].get.map(_.getOrElse(task.skill, 0))
    val incrementAttemptCount = AttemptState[F].modify(_ |+| Map(task.skill -> 1))
    val markSkillLearned      = SkillState[F].modify(_ + task.skill)

    val learnSkill =
      attemptCount.flatMap {
        case attemptsSoFar if attemptsSoFar >= 4 => markSkillLearned
        case _                                   => incrementAttemptCount
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

  val dishes  = Task(Skill("Dishes"))
  val laundry = Task(Skill("Laundry"))

  val tasks: IO[List[Task]] = {
    val taskList = List.fill(10)(dishes) ::: List.fill(6)(laundry)

    IO(scala.util.Random.shuffle(taskList))
  }

  override def run(args: List[String]): IO[ExitCode] = {
    val console = Console.io

    tasks
      .map(solveTasksPure)
      .flatMap {
        case (logs, time) =>
          logs.traverse_(console.putStrLn) *>
            console.putStrLn(time.toString)
      }
  }.as(ExitCode.Success)
}
