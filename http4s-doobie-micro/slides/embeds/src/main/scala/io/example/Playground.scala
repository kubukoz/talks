package io.example

import cats.data.{Validated, ValidatedNel}
import cats.{Applicative, Apply, FlatMap, Monad, NonEmptyParallel, Parallel, effect}
import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import cats.kernel.Monoid

object Playground extends IOApp {

//  val a: ValidatedNel[String, Int] = ???
//  val b: ValidatedNel[String, Boolean] = ???

//  (a, b).tupled

//  def flatMapV[A, B, C, E](fa: Validated[A, B])(f: B => Validated[E, C]): Validated[A, C] =
//    fa.andThen(f)

//  def tupled2[A, B, C](fa: Validated[A, B], fb: Validated[A, C]): Validated[A, (B, C)] =
//    flatMapV(fa)(a => fb.map((a, _)))

//  (
//    Option.empty[String],
//    Option(5),
//    Some(true)
//  ).mapN { (_, b, _) => b }


  import scala.concurrent.duration._
//
//  val list = List(1,2,3)
//  def f(a: Int): IO[List[Int] = ???
//
//  (f(1), f(2)).parTupled
//
//  private val p: cats.Parallel[IO, effect.IO.Par] = Parallel[IO, effect.IO.Par]


//
//
//  list.parTraverse(f)



//  Option.empty[String] *> Option(5) <* Some(true)

  override def run(args: List[String]): IO[ExitCode] = {
    IO(println("dupa"))
    //your code here
  }.as(ExitCode.Success)
}
