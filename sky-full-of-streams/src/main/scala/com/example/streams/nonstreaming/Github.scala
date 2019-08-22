package com.example.streams.nonstreaming

import cats.effect.IO
import cats.implicits._
import com.example.streams.core._
import com.example.streams.core.types._

class Github(projects: Projects, issues: Issues) {

  def findFirstWithMatchingIssue(predicate: Issue => Boolean): IO[Option[Project]] = {
    def go(afterProject: Option[ProjectId]): IO[Option[Project]] =
      projects.getPage(afterProject).map(_.toNel).flatMap {
        _.traverse { projects =>
          val lastId = projects.last.id

          projects
            .findM { project =>
              hasIssueMatching(project.id, predicate, None)
            }
            .flatMap {
              case Some(proj) => proj.some.pure[IO]
              case None       => go(lastId.some)
            }
        }.map(_.flatten)
      }

    go(None)
  }

  private def hasIssueMatching(projectId: ProjectId, predicate: Issue => Boolean, afterIssue: Option[IssueId]): IO[Boolean] =
    issues.getPage(projectId, afterIssue).map(_.toNel).flatMap {
      _.existsM {
        case l if l.exists(predicate) => true.pure[IO]
        case l                        => hasIssueMatching(projectId, predicate, l.last.id.some)
      }
    }
}
