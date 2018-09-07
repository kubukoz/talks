trait ContextShift[F[_]] {
  def shift: F[Unit]

  //shift to ec, run fa, shift back
  def evalOn[A](ec: ExecutionContext)(fa: F[A]): F[A]
}

