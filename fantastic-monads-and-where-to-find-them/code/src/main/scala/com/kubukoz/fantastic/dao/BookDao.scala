package com.kubukoz.fantastic.dao

import cats.effect.Sync
import com.kubukoz.fantastic.data.{Book, BookId}

import scala.language.higherKinds
import scala.util.Random

trait BookDao[F[_]] {
  val findAll: F[List[Book]]

  def isRented(bookId: BookId): F[Boolean]

  def findById(bookId: BookId): F[Option[Book]]

  def rentBook(bookId: BookId): F[Unit]

  def saveBook(book: Book): F[BookId]
}

class InMemoryBookDao[F[_]](implicit F: Sync[F]) extends BookDao[F] {
  type IsRented = Boolean

  private var memory: Map[BookId, (Book, IsRented)] = List(
    Book(BookId("1"), "1234567890", "FP in Scala") -> false,
    Book(BookId("2"), "1234567891", "FP in Java")  -> true
  ).map {
    case p @ (book, _) =>
      book.id -> p
  }.toMap

  override val findAll: F[List[Book]] = F.delay {
    memory.values.map(_._1).toList
  }

  override def isRented(bookId: BookId): F[Boolean] = F.delay {
    memory.get(bookId).exists(_._2)
  }

  override def findById(bookId: BookId): F[Option[Book]] = F.delay {
    memory.get(bookId).map(_._1)
  }

  override def rentBook(bookId: BookId): F[Unit] = F.delay {
    memory.get(bookId).foreach {
      case (book, _) =>
        memory = memory.updated(bookId, book -> true)
    }
  }

  override def saveBook(book: Book): F[BookId] = F.delay {
    val newBookId = BookId(Random.nextString(10)) //don't do this at home

    memory += (newBookId -> (book.copy(id = newBookId), false))

    newBookId
  }

}
