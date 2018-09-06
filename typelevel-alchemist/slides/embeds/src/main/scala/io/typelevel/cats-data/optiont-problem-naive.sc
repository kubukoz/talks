//naive approach
def findUsersCarManufacturerOwner(
  name: String): Future[Option[Owner]] = {
  findUser(name).flatMap {
    case None => Future.successful(None)
    case Some(user) =>
      findCar(user.id).flatMap {
        case None => Future.successful(None)
        case Some(car) =>
          findManufacturer(car).flatMap {
            case None => Future.successful(None)
            case Some(manufacturer) =>
              findOwner(manufacturer).map(Some(_))
          }
      }
  }
}
