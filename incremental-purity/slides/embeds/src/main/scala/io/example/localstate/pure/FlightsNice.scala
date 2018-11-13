package io.example.localstate.pure
import java.time.LocalDate

import fs2._

object FlightsNice {

  def findCheapestFlightsForUser(date: LocalDate,
                                 user: User): List[Flight] = {
    def skipCheapest[F[_]](
      allCount: Int): Pipe[F, Flight, Flight] =
      if (isMacbookUser(user)) _.drop(allCount / 2)
      else identity

    def expensify[F[_]]: Pipe[F, Flight, Flight] =
      if (isDateSoon(date))
        _.map(Flight.price.modify(_ * 6))
          .map(Flight.iconColor.set(Color.Red))
      else identity

    val allFlights = findFlights(date)

    Stream
      .emits(allFlights)
      .through(skipCheapest(allFlights.size))
      .through(expensify)
      .compile
      .toList
  }
}
