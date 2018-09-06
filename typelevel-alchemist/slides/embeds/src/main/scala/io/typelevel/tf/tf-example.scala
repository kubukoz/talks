//the Functor typeclass, we'll look at it more later
trait Functor[F[_]] {
  def map[A, B](fa: F[A])(f: A => B): F[B]
}

trait UserDao[F[_]] {
  def save(user: User): F[Unit]
  def find(userId: UserId): F[User]
}

class UserController[F[_]: Functor](dao: UserDao[F]) {

  def findUserName(userId: UserId): F[String] =
    dao.find(userId).map(_.name)
}
