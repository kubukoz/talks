package com.kubukoz.fantastic.routes

import akka.http.scaladsl.marshalling._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import cats.Functor
import cats.data.ValidatedNel
import cats.syntax.functor._
import com.kubukoz.fantastic.dto.{AppError, BookToCreate, RentBookRequest}
import com.kubukoz.fantastic.routes.CustomDirectives._
import com.kubukoz.fantastic.services.BookService
import com.kubukoz.fantastic.syntax.{ToFuture, _}
import de.heikoseeberger.akkahttpcirce.ErrorAccumulatingCirceSupport._
import io.circe.Encoder
import io.circe.generic.auto._

import scala.language.higherKinds

final class MainRoutes[F[_]: ToFuture: Functor](bookService: BookService[F]) {

  val mainRoutes: Route =
    get {
      pathPrefix("all") {
        complete {
          bookService.findBooks
        }
      }
    } ~ pathPrefix("add") {
      entity(as[BookToCreate]) { request =>
        complete {
          handleValidation {
            bookService.addBook(request)
          }
        }
      }
    } ~ post {
      pathPrefix("rent") {
        entity(as[RentBookRequest]) { request =>
          complete {
            bookService.rentBook(request)
          }
        }
      } ~ pathPrefix("rentMany") {
        entity(as[List[RentBookRequest]]) { requests =>
          complete {
            bookService.rentBooks(requests)
          }
        }
      }
    }
}

object CustomDirectives {

  def handleValidation[F[_]: Functor, E <: AppError, T: Encoder](
    result: F[ValidatedNel[E, T]]): F[ToResponseMarshallable] = {
    result.map {
      _.fold(
        e => (StatusCodes.BadRequest, e.toList.map(_.toString)),
        value => value
      )
    }
  }

  implicit def fMarshaller[F[_]: ToFuture, A: ToResponseMarshaller]: ToResponseMarshaller[F[A]] =
    GenericMarshallers.futureMarshaller[A, HttpResponse].compose(_.toFuture)
}
