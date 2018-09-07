abstract class Ref[F[_], A] {

  def get: F[A]

  def set(a: A): F[Unit]

  def getAndSet(a: A): F[A]

  def modify[B](f: A => (A, B)): F[B]
}
