val dishes  = Task(Skill("Dishes"))
val laundry = Task(Skill("Laundry"))

val tasks: IO[List[Task]] = {
  val taskList = List.fill(10)(dishes) ::: List.fill(6)(laundry)

  IO(util.Random.shuffle(taskList))
}
