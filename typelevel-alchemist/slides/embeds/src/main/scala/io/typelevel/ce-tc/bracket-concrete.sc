//def bracketCase[A, B](acquire: F[A])(use: A => F[B])(
//  release: (A, ExitCase[E]) => F[Unit]): F[B]

def bracket[A, B](acquire: F[A])(use: A => F[B])(
  release: A => F[Unit]): F[B] =
  bracketCase(acquire)(use)((a, _) => release(a))

def uncancelable[A](fa: F[A]): F[A] =
  bracket(fa)(pure)(_ => unit)

def guarantee[A](fa: F[A])(finalizer: F[Unit]): F[A] =
  bracket(unit)(_ => fa)(_ => finalizer)

def guaranteeCase[A](fa: F[A])(
  finalizer: ExitCase[E] => F[Unit]): F[A] =
  bracketCase(unit)(_ => fa)((_, e) => finalizer(e))
