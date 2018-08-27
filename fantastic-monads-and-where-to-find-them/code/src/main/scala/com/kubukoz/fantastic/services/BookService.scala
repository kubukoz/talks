package com.kubukoz.fantastic.services

import cats.{Applicative, Monad}
import cats.data.ValidatedNel
import cats.instances.future._
import cats.instances.either._
import cats.instances.list._
import cats.syntax.all._
import com.kubukoz.fantastic.dao.BookDao
import com.kubukoz.fantastic.data.{Book, BookId}
import com.kubukoz.fantastic.dto.BookCreateError.{InvalidISBN, InvalidName}
import com.kubukoz.fantastic.dto.RentError.{BookAlreadyRented, BookNotFound}
import com.kubukoz.fantastic.dto._

import scala.language.higherKinds

class BookService[F[_]: Monad](dao: BookDao[F]) {
  type RentResult = Either[RentError, Unit]

  val findBooks: F[List[Book]] = dao.findAll

  private def validateBook(toCreate: BookToCreate): ValidatedNel[BookCreateError, Book] = {
    val validateISBN = toCreate.isbn.valid.ensure(InvalidISBN)(_.length == 10)
    val validateName = toCreate.name.valid.ensure(InvalidName)(s => (1 to 10).contains(s.length))

    (
      BookId("-").validNel,
      validateISBN.toValidatedNel,
      validateName.toValidatedNel
    ).mapN(Book)
  }

  def addBook(toCreate: BookToCreate): F[ValidatedNel[BookCreateError, BookId]] = {
    validateBook(toCreate).traverse(dao.saveBook)
  }

  def rentBook(request: RentBookRequest): F[RentResult] = {

    //NOT parallel
    (
      dao.findById(request.bookId),
      dao.isRented(request.bookId)
    ).mapN { (bookOpt, isRented) =>
      bookOpt match {
        case None          => (Left(BookNotFound): RentResult).pure[F]
        case _ if isRented => (Left(BookAlreadyRented): RentResult).pure[F]
        case _             => dao.rentBook(request.bookId).map(_.asRight: RentResult)
      }
    }.flatten
  }

  final def rentBooks(request: List[RentBookRequest]): F[List[RentResult]] =
    request.traverse(rentBook)
}
