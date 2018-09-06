def findUsersCarManufacturerOwner(
  name: String): Future[Either[String, Owner]] = {

  for {
    user  <- EitherT(findUser(name))
    car   <- EitherT(findCar(user.id))
    manu  <- EitherT.fromEither[Future](findManufacturer(car))
    owner <- EitherT.liftF(findOwner(manu))
  } yield owner
}.value
