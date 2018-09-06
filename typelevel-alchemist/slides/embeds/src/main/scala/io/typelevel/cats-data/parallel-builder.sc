case class User(id: Long, name: String, age: Int)

case class UserToCreate(name: String, age: Int)

def validateUser(toCreate: UserToCreate): EitherNel[String, User] = {
  val checkName: EitherNel[String, String] =
    toCreate.name.asRight
      .ensure("Name too short")(_.length >= 2)
      .ensure("Name too long")(_.length <= 30)
      .toEitherNel

  val checkAge: ValidatedNel[String, Int] = toCreate.age.asRight
    .ensure("Age too low")(_ >= 18)
    .ensure("Age too high")(_ <= 200)
    .toEitherNel

  (
    0.asRight,
    checkName,
    checkAge
  ).parMapN(User)
}
