package io.example.localstate.pure

import java.time.LocalDate

object FlightsPure {

  def findCheapestFlights(date: LocalDate,
                          user: User): List[Flight] = {
    val flights = findFlights(date)

    val filtered =
      if (isMacbookUser(user))
        //remove the cheapest half of the flights
        flights.drop(flights.size / 2)
      else
        flights

    if (isDateSoon(date))
      filtered
        .map(flight => flight.copy(price = flight.price * 6))
        .map(flight =>
          flight.copy(
            icon = flight.icon.copy(color = Color.Red)))
    else filtered
  }
}
