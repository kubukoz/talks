package com.kubukoz.fantastic.data

import io.circe.{Decoder, Encoder}

case class Book(id: BookId, isbn: String, name: String)

case class BookId(value: String) extends AnyVal

object BookId {
  implicit val encoder: Encoder[BookId] = Encoder[String].contramap(_.value)
  implicit val decoder: Decoder[BookId] = Decoder[String].map(BookId(_))
}
