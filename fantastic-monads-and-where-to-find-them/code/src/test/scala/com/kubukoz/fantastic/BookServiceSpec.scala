package com.kubukoz.fantastic

import cats.data.NonEmptyList
import cats.implicits._
import com.kubukoz.fantastic.dao.InMemoryBookDao
import com.kubukoz.fantastic.data.BookId
import com.kubukoz.fantastic.dto.BookCreateError.{InvalidISBN, InvalidName}
import com.kubukoz.fantastic.dto.RentError.{BookAlreadyRented, BookNotFound}
import com.kubukoz.fantastic.dto.{BookCreateError, BookToCreate, RentBookRequest}
import com.kubukoz.fantastic.services.BookService
import monix.eval.Coeval
import org.scalatest.{Matchers, WordSpec}

class BookServiceSpec extends WordSpec with Matchers {
  "addBook" when {
    "the name is empty" should {
      val service = new BookService(new InMemoryBookDao[Coeval])

      "return an error" in {
        service.addBook(BookToCreate("1234567890", "")).value shouldBe (InvalidName: BookCreateError).invalidNel
      }
    }

    "the name is too long" should {
      val service = new BookService(new InMemoryBookDao[Coeval])

      "return an error" in {
        service
          .addBook(BookToCreate("1234567890", "12345678901"))
          .value shouldBe (InvalidName: BookCreateError).invalidNel
      }
    }

    "the ISBN isn't exactly 10 chars" should {
      val service = new BookService(new InMemoryBookDao[Coeval])

      "return an error" in {
        service
          .addBook(BookToCreate("123456789", "1234567890"))
          .value shouldBe (InvalidISBN: BookCreateError).invalidNel
      }
    }

    "both the name and the ISBN are invalid" should {
      val service = new BookService(new InMemoryBookDao[Coeval])

      "return both errors" in {
        service.addBook(BookToCreate("1", "")).value shouldBe NonEmptyList
          .of[BookCreateError](InvalidISBN, InvalidName)
          .invalid
      }
    }
  }

  "rentBook" when {
    "the book is already rented" should {
      val service = new BookService(new InMemoryBookDao[Coeval])

      "fail" in {
        service.rentBook(RentBookRequest(BookId("2"))).value shouldBe Left(BookAlreadyRented)
      }
    }

    "the book is not rented yet" should {
      val service = new BookService(new InMemoryBookDao[Coeval])

      def rentBookOne(): service.RentResult = service.rentBook(RentBookRequest(BookId("1"))).value

      "succeed once, then fail" in {
        rentBookOne() shouldBe Right((): Unit)
        rentBookOne() shouldBe Left(BookAlreadyRented)
      }
    }

    "the book doesn't exist" should {
      val service = new BookService(new InMemoryBookDao[Coeval])

      "fail" in {
        service.rentBook(RentBookRequest(BookId("3"))).value shouldBe Left(BookNotFound)
      }
    }

    "a book has been created" should {
      val service = new BookService(new InMemoryBookDao[Coeval])

      "succeed" in {
        val newId = service.addBook(BookToCreate("1234567890", "Hello LX")).value.toOption.get

        service.rentBook(RentBookRequest(newId)).value shouldBe Right((): Unit)
      }
    }
  }

  "rentBooks" when {
    "some books are rented, some aren't, some don't exist" should {
      val service = new BookService(new InMemoryBookDao[Coeval])

      "return a list of results in correct order" in {
        service.rentBooks(
          List(
            RentBookRequest(BookId("2")),
            RentBookRequest(BookId("1")),
            RentBookRequest(BookId("3"))
          )
        ).value shouldBe List(Left(BookAlreadyRented), Right((): Unit), Left(BookNotFound))
      }
    }
  }
}
