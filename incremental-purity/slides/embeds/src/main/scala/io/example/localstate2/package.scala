package io.example

package object localstate2 {
  case class Skill(name: String)
  case class Task(skill: Skill)
}
