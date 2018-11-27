trait UserAlg[F[_]] {
  def findById(id: UserId): F[Option[UserRepr]]
}

object UserAlg {

  def slickInstance[F[_]: Functor](implicit dbioToF: DBIO ~> F): UserAlg[F] = new UserAlg[F] {
    private val byId = TableQuery[Users].findBy(_.id)

    override def findById(id: UserId): F[Option[UserRepr]] = {
      dbioToF {
        byId(id).result.headOption
      }.map(toRepr)
    }
  }
}

//somewhere in your main
val db: Database = ???

implicit val transact: DBIO ~> IO = new (DBIO ~> IO) {
  def apply[A](dbio: DBIO[A]): IO[A] = IO.fromFuture(IO(db.run(dbio.transactionally)))
}
