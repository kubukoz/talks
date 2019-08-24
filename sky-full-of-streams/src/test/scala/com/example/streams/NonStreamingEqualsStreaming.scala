package com.example.streams

import cats.effect.IO
import com.example.streams.core.types.IssueId
import com.example.streams.core.types.ProjectId
import com.example.streams.core.types.UserId
import com.example.streams.core.Issue
import com.example.streams.core.Issues
import com.example.streams.core.Project
import com.example.streams.core.Projects
import org.scalatest.WordSpec
import org.scalatestplus.scalacheck.Checkers
import org.scalacheck.Arbitrary
import org.scalacheck.Cogen
import org.scalacheck.Prop
import cats.implicits._

class NonStreamingEqualsStreaming extends WordSpec with Checkers {
  implicit val arbIssue: Arbitrary[Issue] = Arbitrary(Arbitrary.arbTuple2[IssueId, UserId].arbitrary.map(Issue.tupled))
  implicit val cogenIssue: Cogen[Issue] = implicitly[Cogen[(IssueId, UserId)]].contramap(Issue.unapply(_).get)

  implicit val arbProject: Arbitrary[Project] = Arbitrary(Arbitrary.arbitrary[ProjectId].map(Project))

  implicit val arbIssues: Arbitrary[Issues] = Arbitrary {
    Arbitrary.arbitrary[(ProjectId, Option[IssueId]) => List[Issue]].map { f =>
      f(_, _).pure[IO]
    }
  }

  implicit val arbProjects: Arbitrary[Projects] = Arbitrary {
    Arbitrary.arbitrary[Option[ProjectId] => List[Project]].map { f =>
      f(_).pure[IO]
    }
  }

  "nostreaming has the same result as streaming" in {
    val prop = Prop.forAll { (projects: Projects, issues: Issues, predicate: Issue => Boolean) =>
      val non = new nonstreaming.Github(projects, issues)
      val str = new streaming.Github(projects, issues)

      (non.findFirstWithMatchingIssue(predicate), str.findFirstWithMatchingIssue(predicate))
        .mapN {
          _ == _
        }
        .unsafeRunSync()
    }

    check(
      prop,
      MinSuccessful(100)
    )
  }
}
