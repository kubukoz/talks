package io.example.localstate
import java.time.LocalDate

package object pure {
  def findFlights(date: LocalDate): List[Flight]                 = ???
  def findFlightsF[F[_]](date: LocalDate): fs2.Stream[F, Flight] = ???
  def isMacbookUser(user: User): Boolean                         = ???
  def isMacbookUserF[F[_]](user: User): F[Boolean]               = ???
  def isDateSoon(date: LocalDate): Boolean                       = ???
}
