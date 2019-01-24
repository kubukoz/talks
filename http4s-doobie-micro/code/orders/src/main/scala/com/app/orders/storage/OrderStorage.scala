package com.app.orders.storage

import java.util.UUID

import cats.implicits._
import cats.effect.Sync
import cats.effect.concurrent.Ref
import cats.~>
import com.app.orders.{OrderHandle, OrderStatus}

trait OrderStorage[F[_]] { self =>
  def saveOrder: F[OrderHandle]
  def getStatus(orderHandle: OrderHandle): F[Option[OrderStatus]]
  def countOrders: F[Int]

  def mapK[G[_]](f: F ~> G): OrderStorage[G] = new OrderStorage[G] {
    override val saveOrder: G[OrderHandle]                                   = f(self.saveOrder)
    override def getStatus(orderHandle: OrderHandle): G[Option[OrderStatus]] = f(self.getStatus(orderHandle))
    override val countOrders: G[Int]                                         = f(self.countOrders)
  }
}

object OrderStorage {
  def apply[F[_]](implicit F: OrderStorage[F]): OrderStorage[F] = F

  def inMemory[F[_]: Sync]: F[OrderStorage[F]] =
    Ref[F].of(Map.empty[OrderHandle, OrderStatus]).map { ref =>
      new OrderStorage[F] {
        override val saveOrder: F[OrderHandle] =
          Sync[F]
            .delay(UUID.randomUUID())
            .map(OrderHandle)
            .flatTap(newId => ref.update(_ + (newId -> OrderStatus.InProgress)))

        override def getStatus(orderHandle: OrderHandle): F[Option[OrderStatus]] = ref.get.map(_.get(orderHandle))

        override val countOrders: F[Int] = ref.get.map(_.size)
      }
    }
}
