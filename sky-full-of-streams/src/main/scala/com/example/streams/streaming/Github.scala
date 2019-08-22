package com.example.streams.streaming

import com.example.streams.core._
import com.example.streams.core.types._
import cats.Functor
import cats.effect.IO
import cats.implicits._

//example: traverse a list multiple times without effects
//example: traverse a list multiple times with effects
//websockets
//database streaming

class Github(projects: Projects, issues: Issues) {
  import fs2._

  private def filterEval[F[_]: Functor, A](pred: A => F[Boolean]): Pipe[F, A, A] =
    _.evalMap(e => pred(e).map(_.guard[Option].as(e))).unNone

  private def paginateByLastSeen[F[_]: Functor, A](fetch: Option[A] => F[List[A]]): Stream[F, A] =
    Stream.unfoldChunkEval[F, Option[A], A](none[A]) {
      fetch(_).map { items =>
        (Chunk.seq(items), items.lastOption.map(_.some)).sequence
      }
    }

  def findFirstWithMatchingIssue(predicate: Issue => Boolean): IO[Option[Project]] = {
    val projectStream: Stream[IO, Project] = paginateByLastSeen[IO, Project](proj => projects.getPage(proj.map(_.id)))

    def issueStream(project: ProjectId): Stream[IO, Issue] =
      paginateByLastSeen[IO, Issue](issue => issues.getPage(project, issue.map(_.id)))

    projectStream
      .through(filterEval { project =>
        issueStream(project.id).exists(predicate).compile.last.map(_.getOrElse(false))
      })
      .head
      .compile
      .last
  }
}
