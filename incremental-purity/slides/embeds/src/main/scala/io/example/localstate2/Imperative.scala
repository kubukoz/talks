package io.example.localstate2

object Imperative {
  import scala.concurrent.duration._

  def solveTasks(tasks: List[Task]): FiniteDuration = {
    var timeSoFar                 = 0.seconds
    var skillsKnown: Set[Skill]   = Set.empty
    var attempts: Map[Skill, Int] = Map.empty

    tasks.foreach { task =>
      if (skillsKnown.contains(task.skill)) {
        println(s"I know how to do ${task.skill.name}! Adding only 1 second")
        timeSoFar += 1.second
      } else {
        attempts.get(task.skill) match {
          case Some(attemptsSoFar) if attemptsSoFar >= 4 => skillsKnown += task.skill
          case Some(attemptsSoFar)                       => attempts += (task.skill -> (attemptsSoFar + 1))
          case None                                      => attempts += (task.skill -> 1)
        }

        println(s"Adding 10 secs for ${task.skill.name}")
        timeSoFar += 10.seconds
      }
    }

    timeSoFar
  }

  val dishes  = Task(Skill("Dishes"))
  val laundry = Task(Skill("Laundry"))

  val tasks: List[Task] = util.Random.shuffle(List.fill(10)(dishes) ::: List.fill(6)(laundry))

  def main(args: Array[String]): Unit = {
    println(solveTasks(tasks))
  }
}
