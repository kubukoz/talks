import com.olegpy.meow.hierarchy._

type HasSkillState[F[_]] = MonadState[F, MyState]

private def work[F[_]: HasSkillState: FlatMap: Log](task: Task): F[FiniteDuration] = {
  val knowsRequiredSkill: F[Boolean] = SkillState[F].get.map(_.contains(task.skill))

  knowsRequiredSkill.ifM(
    useKnownSkill[F](task),
    solveAndLearnSkill[F](task)
  )
}
