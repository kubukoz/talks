package com.example.streams

import cats.effect.IO
import cats.implicits._
import com.example.streams.core.Issue
import com.example.streams.core.Project
import com.example.streams.core.types.IssueId
import com.example.streams.core.types.ProjectId
import com.example.streams.core.types.UserId

class Traversing(
  findProject: ProjectId => IO[Project],
  findIssues: Project => IO[List[IssueId]],
  findIssue: IssueId => IO[Issue],
  allProjectIds: IO[List[ProjectId]],
  me: UserId
) {

  def allMyIssuesInProjectsWithExclusions(
    excludedProject: Project => Boolean,
    excludedIssue: Issue => Boolean
  ): IO[List[Issue]] =
    allProjectIds
      .flatMap(_.traverse(findProject))
      .map(_.filterNot(excludedProject))
      .flatMap(_.flatTraverse(findIssues))
      .flatMap(_.traverse(findIssue))
      .map(_.filterNot(excludedIssue))
      .map(_.filter(_.creator === me))

  import fs2._

  def withFs2(
    excludedProject: Project => Boolean,
    excludedIssue: Issue => Boolean
  ): IO[List[Issue]] =
    Stream
      .eval(allProjectIds)
      .flatMap(Stream.emits)
      .evalMap(findProject)
      .filter(!excludedProject(_))
      .evalMap(findIssues)
      .flatMap(Stream.emits)
      .evalMap(findIssue)
      .filter(!excludedIssue(_))
      .filter(_.creator === me)
      .compile
      .toList

  def withFs2Pipes(
    excludedProject: Project => Boolean,
    excludedIssue: Issue => Boolean
  ): IO[List[Issue]] = {
    def issues: Pipe[IO, Project, Issue] =
      _.evalMap(findIssues).flatMap(Stream.emits).evalMap(findIssue)

    val projects: Stream[IO, Project] =
      Stream
        .eval(allProjectIds)
        .flatMap(Stream.emits)
        .evalMap(findProject)

    projects
      .filter(!excludedProject(_))
      .through(issues)
      .filter(!excludedIssue(_))
      .filter(_.creator === me)
      .compile
      .toList
  }

}
