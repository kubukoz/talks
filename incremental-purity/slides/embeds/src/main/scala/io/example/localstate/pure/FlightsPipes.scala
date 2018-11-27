package io.example.localstate.pure
import java.time.LocalDate

import cats.data.NonEmptyList
import cats.implicits._
import cats.{Endo, SemigroupK}

object FlightsPipes {

  def findCheapestFlightsForUser(date: LocalDate,
                                 user: User): List[Flight] = {
    type Pipe[A] = List[A] => List[A]

    //Endo[A] is A => A
    implicit val pipeSemK: SemigroupK[Pipe] =
      SemigroupK[Endo].compose[List]

    def skipCheapest(allCount: Int): Pipe[Flight] =
      if (isMacbookUser(user)) _.drop(allCount / 2)
      else identity

    val expensify: Pipe[Flight] =
      if (isDateSoon(date))
        _.map(Flight.price.modify(_ * 6))
          .map(Flight.iconColor.set(Color.Red))
      else identity

    val allFlights = findFlights(date)

    val mods = NonEmptyList.of(
      expensify,
      skipCheapest(allFlights.size)
    )

    //equivalent to `expensify(skipCheapest(allFlights.size)(allFlights)`
    mods.reduceK.apply(allFlights)
  }
}
