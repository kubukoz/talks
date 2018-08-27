package com.kubukoz

import monix.eval.Task

import scala.language.higherKinds

package object fantastic {
  type Result[T] = Task[T]
}
