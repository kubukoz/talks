package com.app.orders.http4s
import org.http4s.Uri
import pureconfig.ConfigReader
import pureconfig.error.CannotConvert

object UriInstances {
  //inspired by pureconfig's http4s module
  implicit val uriReader: ConfigReader[Uri] =
    ConfigReader.fromString(str =>
      Uri.fromString(str).fold(err => Left(CannotConvert(str, "Uri", err.sanitized)), uri => Right(uri)))
}
