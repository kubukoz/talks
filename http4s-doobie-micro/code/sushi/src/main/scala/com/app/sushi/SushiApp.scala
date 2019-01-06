package com.app.sushi

import cats.{Applicative, Monad}
import cats.implicits._
import cats.effect._
import doobie.hikari.HikariTransactor
import doobie._
import io.circe.Encoder
import org.flywaydb.core.Flyway
import org.http4s.{EntityEncoder, HttpRoutes}
import org.http4s.dsl.Http4sDsl
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.circe._
import org.http4s.implicits._

object SushiApp extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    new SushiServer[IO].run
}

class SushiServer[F[_]: Timer: ContextShift](implicit F: ConcurrentEffect[F]) {

  private val dbUrl = "jdbc:postgresql://localhost/postgres"
  private val runMigrations: F[Unit] =
    F.delay {
      Flyway.configure().dataSource(dbUrl, "postgres", "postgres").load().migrate()
    }.void

  private val serverResource: Resource[F, Unit] = for {
    connectEc  <- ExecutionContexts.fixedThreadPool[F](10)
    transactEc <- ExecutionContexts.cachedThreadPool[F]
    transactor <- HikariTransactor
      .newHikariTransactor[F]("org.postgresql.Driver", dbUrl, "postgres", "postgres", connectEc, transactEc)
    _ <- Resource.liftF(runMigrations)
    _ <- {
      implicit val xa: Transactor[F] = transactor

      BlazeServerBuilder[F].bindHttp(port = 3000).withHttpApp(SushiModule.make[F].routes.orNotFound).resource
    }
  } yield ()

  val run: F[Nothing] = serverResource.use[Nothing](_ => F.never)
}

trait SushiModule[F[_]] {
  def routes: HttpRoutes[F]
}

object SushiModule {

  def make[F[_]: Sync: Transactor]: SushiModule[F] = {
    implicit val repository: SushiRepository[F] = SushiRepository.instance[F]

    new SushiModule[F] {
      override val routes: HttpRoutes[F] = SushiRoutes.instance[F].routes
    }
  }
}

trait SushiRoutes[F[_]] {
  def routes: HttpRoutes[F]
}

object SushiRoutes {
  implicit def entityEncoderForCirce[F[_]: Applicative, A: Encoder]: EntityEncoder[F, A] = jsonEncoderOf

  def instance[F[_]: Sync: SushiRepository]: SushiRoutes[F] = new SushiRoutes[F] with Http4sDsl[F] {
    override val routes: HttpRoutes[F] = HttpRoutes.of {
      case GET -> Root / "kinds" / "by-name" / name =>
        SushiRepository[F].findByName(name).flatMap {
          case None       => NotFound(show"A kind by the requested name was not found: $name")
          case Some(kind) => Ok(kind)
        }
    }
  }
}

trait SushiRepository[F[_]] {
  def findByName(name: String): F[Option[SushiKind]]
}

object SushiRepository {
  def apply[F[_]](implicit F: SushiRepository[F]): SushiRepository[F] = F

  def instance[F[_]: Monad](implicit xa: Transactor[F]): SushiRepository[F] = new SushiRepository[F] {

    override def findByName(name: String): F[Option[SushiKind]] = {
      import doobie.implicits._

      sql"select kind.name, kind.set_size, kind.price from sushi_kinds kind where kind.name = $name"
        .query[SushiKind]
        .option
        .transact(xa)
    }
  }
}
