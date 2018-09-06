def findUser(name: String): Future[Either[String, User]]
def findCar(userId: Long): Future[Either[String, Car]]
def findManufacturer(car: Car): Either[String, Manufacturer]
def findOwner(manufacturer: Manufacturer): Future[Owner]
