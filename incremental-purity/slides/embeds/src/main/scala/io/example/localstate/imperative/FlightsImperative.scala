package io.example.localstate.imperative

import java.time.LocalDate

object FlightsImperative {

  def findCheapestFlights(date: LocalDate,
                          user: User): List[Flight] = {
    var flights = findFlights(date)

    if (isMacbookUser(user))
      //remove the cheapest half of the flights
      flights = flights.drop(flights.size / 2)

    if (isDateSoon(date))
      flights = flights.map { flight =>
        flight.price *= 6
        flight.icon.color = Color.Red

        flight
      }

    flights
  }
}
