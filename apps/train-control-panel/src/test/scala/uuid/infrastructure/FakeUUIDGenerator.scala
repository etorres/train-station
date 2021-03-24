package es.eriktorr.train_station
package uuid.infrastructure

import uuid.UUIDGenerator
import uuid.infrastructure.FakeUUIDGenerator.UUIDGeneratorState

import cats.data.NonEmptyList
import cats.effect.Sync
import cats.effect.concurrent.Ref
import cats.implicits._

import java.util.UUID

final class FakeUUIDGenerator[F[_]: Sync] private[infrastructure] (
  val ref: Ref[F, UUIDGeneratorState]
) extends UUIDGenerator[F] {
  @SuppressWarnings(Array("org.wartremover.warts.ListAppend"))
  override def nextUuid: F[UUID] =
    for {
      current <- ref.get
      (uuidsHead, uuidsTail) = (current.uuids.head, current.uuids.tail)
      _ <- ref.set(current.copy {
        uuidsTail match {
          case ::(head, next) => NonEmptyList(head, next :+ uuidsHead)
          case Nil => NonEmptyList.one(uuidsHead)
        }
      })
    } yield uuidsHead
}

object FakeUUIDGenerator {
  final case class UUIDGeneratorState(uuids: NonEmptyList[UUID])

  object UUIDGeneratorState {
    def refFrom[F[_]: Sync](str: String): F[Ref[F, UUIDGeneratorState]] =
      Ref.of[F, UUIDGeneratorState](
        UUIDGeneratorState(NonEmptyList.one(UUID.fromString(str)))
      )
  }

  def impl[F[_]: Sync](ref: Ref[F, UUIDGeneratorState]): UUIDGenerator[F] =
    new FakeUUIDGenerator[F](ref)
}
