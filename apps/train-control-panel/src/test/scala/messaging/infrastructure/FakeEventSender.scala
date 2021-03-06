package es.eriktorr.train_station
package messaging.infrastructure

import event.Event
import messaging.EventSender
import messaging.infrastructure.FakeEventSender.EventSenderState

import cats.effect.Sync
import cats.effect.concurrent.Ref
import cats.implicits._

final class FakeEventSender[F[_]: Sync] private[messaging] (val ref: Ref[F, EventSenderState])
    extends EventSender[F] {
  override def send(event: Event): F[Unit] =
    ref.get.flatMap(current => ref.set(current.copy(event :: current.events)))
}

object FakeEventSender {
  final case class EventSenderState(events: List[Event])

  object EventSenderState {
    def refEmpty[F[_]: Sync]: F[Ref[F, EventSenderState]] =
      Ref.of[F, EventSenderState](EventSenderState(List.empty))
  }

  def impl[F[_]: Sync](ref: Ref[F, EventSenderState]): FakeEventSender[F] =
    new FakeEventSender[F](ref)
}
