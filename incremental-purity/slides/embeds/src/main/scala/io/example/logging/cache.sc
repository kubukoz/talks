object UserService {

  def make[F[_]: Sync]: F[UserService[F]] =
    Cache.createCache[IO, String, Int](Some(Cache.TimeSpec.unsafeFromDuration(1.second))).map { cache =>
      val getCount = memoizeSuccess("userCount") {
        ???
      }
    }
}

def memoizeSuccess[F[_], K, V](key: K)(fv: F[V])(implicit Cache: Cache[F, K, V]): F[V] = {
  Cache.lookup(key).flatMap {
    case Some(v) => v.pure[F]
    case None    => fv.flatTap(Cache.insert(key, _))
  }
}
