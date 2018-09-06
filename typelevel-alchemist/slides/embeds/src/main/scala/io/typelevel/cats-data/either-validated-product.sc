implicit def eitherApplicative[E]: Applicative[Either[E, ?]] =
  new Applicative[Either[E, ?]] {

    def product[A, B](fa: Either[E, A],
                      fb: Either[E, B]): Either[E, (A, B)] =
      (fa, fb) match {
        case (Right(a), Right(b)) => Right((a, b))
        case (Left(e), _)         => Left(e)
        case (_, Left(e))         => Left(e)
      }
  }

implicit def validatedApplicative[E: Semigroup]
  : Applicative[Validated[E, ?]] =
  new Applicative[Validated[E, ?]] {

    def product[A, B](
      fa: Validated[E, A],
      fb: Validated[E, B]): Validated[E, (A, B)] =
      (fa, fb) match {
        case (Valid(a), Valid(b))       => Valid((a, b))
        case (Invalid(e1), Invalid(e2)) => Invalid(e1 |+| e2)
        case (Invalid(e1), _)           => Invalid(e1)
        case (_, Invalid(e2))           => Invalid(e2)
      }

  }
