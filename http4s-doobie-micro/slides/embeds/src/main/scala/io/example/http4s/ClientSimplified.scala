package io.example.http4s.client
import cats.effect.{Bracket, Resource}
import org.http4s.{Request, Response}

trait Client[F[_]] {
  def run(req: Request[F]): Resource[F, Response[F]]
  //...
}

object Client {

  def apply[F[_]](
    f: Request[F] => Resource[F, Response[F]]
  ): Client[F] = req => f(req)
}
