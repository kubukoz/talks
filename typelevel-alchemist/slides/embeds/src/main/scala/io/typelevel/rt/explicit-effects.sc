trait MyService {
  def check(thing: Thing): Either[ValidationError, CheckedThing]

  def insert(thing: CheckedThing): IO[InsertedThing]
}
