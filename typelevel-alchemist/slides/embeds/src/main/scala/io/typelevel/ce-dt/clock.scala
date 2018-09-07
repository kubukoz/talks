trait Clock[F[_]] {
  def realTime(unit: TimeUnit): F[Long]

  def monotonic(unit: TimeUnit): F[Long]
}
