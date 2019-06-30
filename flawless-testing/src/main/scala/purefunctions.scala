package purefunctions

import cats.implicits._
import cats.effect.IO
import cats.data.NonEmptyList
import cats.kernel.Eq
import eu.timepit.refined._
import eu.timepit.refined.types.numeric.PosInt
import eu.timepit.refined.auto._
import cats.effect.concurrent.Ref
import cats.kernel.Semigroup
import cats.Show

object purefunctions {
  type PositiveInt = PosInt

  object impure {
    class CreditCard(var credit: Int)

    def pay(amount: PositiveInt, card: CreditCard): Unit =
      card.credit -= amount.value

    def program() = {
      val card = new CreditCard(300)

      pay(100, card)
      println(card.credit) //0
    }

    def programRefactored() = {
      pay(100, new CreditCard(300))
      println(new CreditCard(300).credit) //0
    }
  }

  object pure {
    case class CreditCard(credit: Int)

    def pay(amount: PositiveInt, card: CreditCard): CreditCard =
      card.copy(credit = card.credit - amount.value)

    def program() = {
      val card = CreditCard(300)

      val newCard = pay(100, card)
      println(newCard.credit) //200
      println(card.credit) //300
    }

    def programRefactored() = {
      val newCard = pay(100, CreditCard(300))
      println(newCard.credit) //200
      println(CreditCard(300).credit) //300
    }
  }

  case class CardId(value: Long) extends AnyVal
  case class CreditCardDB(credit: Int)

  object CreditCardDB {
    implicit val semigroup: Semigroup[CreditCardDB] = (c1, c2) => CreditCardDB(c1.credit |+| c2.credit)
  }

  object realworld {
    def putStrLn[A: Show](a: A): IO[Unit] = IO { println(a.show) }

    type Database = Ref[IO, Map[CardId, CreditCardDB]]
    def newDatabase(initialCredit: Int): IO[Database] = Ref[IO].of(Map(CardId(1) -> CreditCardDB(initialCredit)))

    def pay(amount: PositiveInt, card: CardId, db: Database): IO[Unit] =
      db.update(_ |+| Map(card -> CreditCardDB(amount.value)))

    def getCredit(card: CardId, db: Database): IO[Int] =
      db.get.map(_.get(card).map(_.credit).getOrElse(0))

    def program() = newDatabase(initialCredit = 300).flatMap { db =>
      val card = CardId(1)

      for {
        _      <- pay(100, card, db)
        _      <- pay(100, card, db)
        credit <- getCredit(card, db)
        _      <- putStrLn(credit) //-200
      } yield ()
    }

    def programRefactored() = newDatabase(initialCredit = 300).flatMap { db =>
      val card = CardId(1)

      val pay100 = pay(100, card, db)

      for {
        _      <- pay100
        _      <- pay100
        credit <- getCredit(card, db)
        _      <- putStrLn(credit)
      } yield ()
    }

  }
}
