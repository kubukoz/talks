package io.example.localstate
import java.time.LocalDate

package object pure {
  def findFlights(date: LocalDate): List[Flight] = ???
  def isMacbookUser(user: User): Boolean         = ???
  def isDateSoon(date: LocalDate): Boolean       = ???
}
