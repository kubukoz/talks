package com.app.orders.storage

import java.util.UUID
import cats.implicits._
import cats.effect.Sync
import cats.effect.concurrent.Ref
import com.app.orders.{OrderHandle, OrderStatus}

trait OrderStorage[F[_]] {
  def saveOrder: F[OrderHandle]
  def getStatus(orderHandle: OrderHandle): F[Option[OrderStatus]]
}

object OrderStorage {
  def apply[F[_]](implicit F: OrderStorage[F]): OrderStorage[F] = F

  def inMemory[F[_]: Sync, G[_]: Sync]: F[OrderStorage[G]] =
    Ref.in[F, G, Map[OrderHandle, OrderStatus]](Map.empty).map { ref =>
      new OrderStorage[G] {
        override val saveOrder: G[OrderHandle] =
          Sync[G]
            .delay(UUID.randomUUID())
            .map(OrderHandle)
            .flatTap(newId => ref.update(_ + (newId -> OrderStatus.InProgress)))

        override def getStatus(orderHandle: OrderHandle): G[Option[OrderStatus]] = ref.get.map(_.get(orderHandle))
      }
    }
}