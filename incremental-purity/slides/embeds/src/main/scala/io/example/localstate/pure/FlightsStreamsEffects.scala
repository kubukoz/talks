package io.example.localstate.pure
import java.time.LocalDate

import cats.Functor
import cats.implicits._
import fs2.{Pipe, Stream}

object FlightsStreamsEffects {

  def findCheapestFlightsForUser[F[_]: Functor](date: LocalDate, user: User): Stream[F, Flight] = {
    def skipCheapest(allCount: Int): Pipe[F, Flight, Flight] =
      Pipe.eval {
        isMacbookUserF[F](user).map[Pipe[F, Flight, Flight]] {
          case true  => _.drop(allCount / 2)
          case false => identity
        }
      }

    val expensify: Pipe[F, Flight, Flight] =
      if (isDateSoon(date))
        _.map(Flight.price.modify(_ * 6))
          .map(Flight.iconColor.set(Color.Red))
      else identity

    val allFlights = findFlightsF[F](date)

    val countF = allFlights.as(1).foldMonoid

    countF.flatMap[F, Flight] { count =>
      allFlights
        .through(skipCheapest(count))
        .through(expensify)
    }
  }

  object Pipe {
    //PR to fs2 incoming ;)
    def eval[F[_], A, B](pf: F[Pipe[F, A, B]]): Pipe[F, A, B] = stream =>
      Stream.eval(pf).flatMap(stream.through(_))
  }
}
