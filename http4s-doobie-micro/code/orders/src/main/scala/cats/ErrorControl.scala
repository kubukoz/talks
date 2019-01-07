package cats

import cats.data.EitherT

//based off https://github.com/typelevel/cats/pull/2231

/**
  * A type class for principled error handling.
  * `ErrorControl` is designed to be a supplement to `MonadError` with more precise typing.
  * It is defined as a relationship between an error-handling type `F[A]` and a non-error-handling type `G[A]`.
  * This means a value of `F[A]` is able to produce either a value of `A` or an error of type `E`.
  * Unlike `MonadError`'s `handleError` method, the `controlError` function defined in this type class
  * will yield a value that free of any errors, since they've all been handled.
  *
  * Must adhere to the laws defined in cats.laws.ErrorControlLaws.
  */
trait ErrorControl[F[_], G[_], E] extends Serializable {

  /**
    * The MonadError instance for F[_]
    */
  def monadErrorF: MonadError[F, E]

  /**
    * The Monad instance for G[_]
    */
  def monadG: Monad[G]

  /**
    * Handle any error and recover from it, by mapping it to an
    * error-free `G[A]` value.
    *
    * Similar to `handleErrorWith` on `ApplicativeError`
    *
    * Example:
    * {{{
    * scala> import cats._, data._, implicits._
    *
    * scala> EitherT(List(42.asRight, "Error!".asLeft, 7.asRight)).controlError(err => List(0, -1))
    * res0: List[Int] = List(42, 0, -1, 7)
    * }}}
    */
  def controlError[A](fa: F[A])(f: E => G[A]): G[A]

  /**
    * Injects this error-free `G[A]` value into an `F[A]`.
    */
  def accept[A](ga: G[A]): F[A]

  /**
    * Transform this `F[A]` value, by mapping either the error or the valid value to a new error-free `G[B]`.
    */
  def control[A, B](fa: F[A])(f: Either[E, A] => G[B]): G[B] =
    monadG.flatMap(trial(fa))(f)

  /**
    * Handle errors by turning them into [[scala.util.Either]] values inside `G`.
    *
    * If there is no error, then an `scala.util.Right` value will be returned.
    *
    * All non-fatal errors should be handled by this method.
    *
    * Similar to `attempt` on `ApplicativeError`.
    */
  def trial[A](fa: F[A]): G[Either[E, A]] =
    intercept(monadErrorF.map(fa)(Right(_): Either[E, A]))(Left(_))

  /**
    * Like [[trial]], but returns inside the [[cats.data.EitherT]] monad transformer instead.
    */
  def trialT[A](fa: F[A]): EitherT[G, E, A] =
    EitherT(trial(fa))

  /**
    * Handle any error and recover from it, by mapping it to `A`.
    *
    * Similar to `handleError` on `ApplicativeError`
    *
    * Example:
    * {{{
    * scala> import cats._, data._, implicits._
    *
    * scala> EitherT(List(42.asRight, "Error!".asLeft, 7.asRight, "Another error".asLeft)).intercept(_ => 0)
    * res0: List[Int] = List(42, 0, 7, 0)
    * }}}
    */
  def intercept[A](fa: F[A])(f: E => A): G[A] =
    controlError(fa)(f andThen monadG.pure)

  /**
    * The inverse of [[trial]].
    *
    * Example:
    * {{{
    * scala> import cats._, data._, implicits._
    *
    * scala> List(42.asRight, "Error!".asLeft, 7.asRight).absolve[EitherT[List, String, ?]]
    * res0: EitherT[List, String, Int] = EitherT(List(Right(42), Left(Error!), Right(7)))
    * }}}
    */
  def absolve[A](gea: G[Either[E, A]]): F[A] =
    monadErrorF.flatMap(accept(gea))(_.fold(monadErrorF.raiseError, monadErrorF.pure))

  /**
    * Turns a successful value into an error specified by the `error` function if it does not satisfy a given predicate.
    *
    * Example:
    * {{{
    * scala> import cats._, data._, implicits._
    *
    * scala> List(42, 23, -4).assure[EitherT[List, String, ?]](n => if (n < 0) Some("Negative number: " + n) else None)
    * res0: EitherT[List, String, Int] = EitherT(List(Right(42), Right(23), Left(Negative number: -4)))
    * }}}
    */
  def assure[A](ga: G[A])(error: A => Option[E]): F[A] =
    monadErrorF.flatMap(accept(ga))(a =>
      error(a) match {
        case Some(e) => monadErrorF.raiseError(e)
        case None    => monadErrorF.pure(a)
    })

}

object ErrorControl {
  def apply[F[_], G[_], E](implicit ev: ErrorControl[F, G, E]): ErrorControl[F, G, E] = ev

  implicit def catsErrorControlForEitherT[F[_]: Monad, E]: ErrorControl[EitherT[F, E, ?], F, E] =
    new ErrorControl[EitherT[F, E, ?], F, E] {
      val monadErrorF: MonadError[EitherT[F, E, ?], E] = EitherT.catsDataMonadErrorForEitherT
      val monadG: Monad[F]                             = Monad[F]

      def controlError[A](fa: EitherT[F, E, A])(f: E => F[A]): F[A] =
        Monad[F].flatMap(fa.value) {
          case Left(e)  => f(e)
          case Right(a) => monadG.pure(a)
        }

      def accept[A](ga: F[A]): EitherT[F, E, A] =
        EitherT.liftF(ga)
    }
}
