package com.app.orders.util

import cats.mtl.ApplicativeAsk

trait AskFunctions[A] {
  type Ask[F[_]] = ApplicativeAsk[F, A]
  def Ask[F[_]](implicit F: Ask[F]): Ask[F] = F
}
