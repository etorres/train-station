package es.eriktorr.train_station
package uuid

import cats.effect.Sync

import java.util.UUID

trait UUIDGenerator[F[_]] {
  def nextUuid: F[UUID]
}

object UUIDGenerator {
  def apply[F[_]](implicit ev: UUIDGenerator[F]): UUIDGenerator[F] = ev

  implicit def syncUUIDs[F[_]: Sync]: UUIDGenerator[F] = new UUIDGenerator[F] {
    override def nextUuid: F[UUID] = F.delay(UUID.randomUUID())
  }
}
