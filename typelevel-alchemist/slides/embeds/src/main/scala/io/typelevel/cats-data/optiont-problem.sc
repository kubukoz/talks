def findUser(name: String): Future[Option[User]]
def findCar(userId: Long): Future[Option[Car]]
def findManufacturer(car: Car): Future[Option[Manufacturer]]
def findOwner(manufacturer: Manufacturer): Future[Owner]
