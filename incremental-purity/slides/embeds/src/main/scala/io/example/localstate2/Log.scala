package io.example.localstate2

trait Log[F[_]] {
  def putStrLn(s: String): F[Unit]
}

object Log {
  def apply[F[_]](implicit F: Log[F]): Log[F] = F
}
