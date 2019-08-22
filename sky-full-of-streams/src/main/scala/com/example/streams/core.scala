package com.example.streams
import cats.effect.IO

object core {

  object types {
    type ProjectId = String
    type IssueId = String
    type UserId = String
  }
  import types._

  final case class Project(id: ProjectId)
  final case class Issue(id: IssueId, creator: UserId)

  trait Projects {
    def getPage(afterProject: Option[ProjectId]): IO[List[Project]]
  }

  trait Issues {
    def getPage(projectId: ProjectId, afterIssue: Option[IssueId]): IO[List[Issue]]
  }

}
