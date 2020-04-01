package com.kubukoz.tagless.examples

import cats.tagless.finalAlg
import cats.tagless.autoInstrument
import cats.implicits._
import cats.tagless.autoApplyK
import cats.tagless.autoProductNK
import cats.tagless.autoFunctorK
import io.chrisdavenport.log4cats.Logger
import cats.Monad
import natchez.Trace
import cats.Apply
import cats.effect.Timer
import scala.concurrent.duration._

final case class User(name: String)
final case class UserId()

@finalAlg
@autoApplyK
@autoProductNK
@autoInstrument
trait UserService[F[_]] {
  def findUser(id: UserId): F[Option[User]]
  def findAll: F[List[User]]
}

object UserService {

  val constant: UserService[cats.Id] = new UserService[cats.Id] {
    val theUser = User("Foo")
    val theUser2 = User("Bar")

    def findUser(id: UserId): Option[User] = theUser.some
    val findAll: List[User] = List(theUser, theUser2)
  }
}

@autoInstrument
@autoFunctorK
trait MainService[F[_]] {
  def sendMessageToEach(text: String): F[Unit]
}

object MainService {

  def instance[F[_]: Monad](users: UserService[F], mail: MailService[F]): MainService[F] =
    new MainService[F] {

      def sendMessageToEach(text: String): F[Unit] =
        users
          .findAll
          .map(_.map(u => s"Message for ${u.name}: $text"))
          .flatMap(_.traverse_(mail.sendMessage))
    }

}

@autoInstrument
@autoFunctorK
trait MailService[F[_]] {
  def sendMessage(text: String): F[Unit]
}

object MailService {

  def logging[F[_]: Logger: Trace: Apply: Timer]: MailService[F] =
    msg =>
      Trace[F].put("msg" -> msg) *> Logger[F].info("Sending: " + msg) *> Timer[F].sleep(
        100.millis
      )
}
