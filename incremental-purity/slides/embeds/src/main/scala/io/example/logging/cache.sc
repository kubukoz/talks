trait UserService[F[_]] { def getCount: F[Int] }

object UserService {

  def make[F[_]: Sync: Timer]: F[UserService[F]] =
    Cache.createCache[F, String, Int](Some(Cache.TimeSpec.unsafeFromDuration(1.second))).map { cache =>
      new UserService[F] {
        val getCount: F[Int] = {
          val job: F[Int] = Timer[F].sleep(5.seconds).as(10)

          memoizeSuccess("userCount") {
            job
          }
        }
      }
    }
<span class="fragment">
  val example: IO[Boolean] = for {
    service <- make[IO]
    result1 <- service.getCount //takes 5 seconds
    result2 <- service.getCount //takes ~0 seconds
  } yield result1 == result2</span>
}

def memoizeSuccess[F[_], K, V](key: K)(fv: F[V])(implicit Cache: Cache[F, K, V]): F[V] = {
  Cache.lookup(key).flatMap {
    case Some(v) => v.pure[F]
    case None    => fv.flatTap(Cache.insert(key, _))
  }
}
