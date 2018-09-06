def findUsersCarManufacturerOwner(
  name: String): Future[Option[Owner]] = {

  for {
    user         <- OptionT(findUser(name))
    car          <- OptionT(findCar(user.id))
    manufacturer <- OptionT(findManufacturer(car))
    owner        <- OptionT.liftF(findOwner(manufacturer))
  } yield owner
}.value
