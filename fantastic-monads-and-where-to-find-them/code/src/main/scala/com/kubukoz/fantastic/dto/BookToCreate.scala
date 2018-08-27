package com.kubukoz.fantastic.dto

case class BookToCreate(isbn: String, name: String)

sealed trait BookCreateError extends AppError

object BookCreateError {

  case object InvalidName extends BookCreateError

  case object InvalidISBN extends BookCreateError
}




