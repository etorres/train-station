package es.eriktorr.train_station
package infrastructure

import uuid.UUIDGenerator

import cats.data.NonEmptyList
import cats.effect.{IO, Sync}
import cats.effect.concurrent.Ref
import cats.implicits._

import java.util.UUID

final case class UUIDGeneratorState(uuids: NonEmptyList[UUID])

object UUIDGeneratorState {
  def refFrom(str: String): IO[Ref[IO, UUIDGeneratorState]] = Ref.of[IO, UUIDGeneratorState](
    UUIDGeneratorState(NonEmptyList.one(UUID.fromString(str)))
  )
}

final class FakeUUIDGenerator[F[_]: Sync] private[infrastructure] (
  val ref: Ref[F, UUIDGeneratorState]
) extends UUIDGenerator[F] {
  @SuppressWarnings(Array("org.wartremover.warts.ListAppend"))
  override def next: F[UUID] =
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
  def impl[F[_]: Sync](ref: Ref[F, UUIDGeneratorState]): UUIDGenerator[F] =
    new FakeUUIDGenerator[F](ref)
}
