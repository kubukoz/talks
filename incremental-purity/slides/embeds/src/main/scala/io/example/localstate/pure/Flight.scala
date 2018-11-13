package io.example.localstate.pure

import monocle.Lens
import monocle.macros.Lenses

@Lenses
case class Flight(price: Long, icon: FlightIcon)

object Flight {

  val iconColor: Lens[Flight, Color] =
    icon.composeLens(FlightIcon.color)
}

@Lenses
case class FlightIcon(color: Color)

case class User()
