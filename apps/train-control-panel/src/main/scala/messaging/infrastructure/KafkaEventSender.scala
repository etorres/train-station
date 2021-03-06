package es.eriktorr.train_station
package messaging.infrastructure

import event.Event
import event.Event.Departed
import messaging.EventSender

import cats.effect._
import cats.implicits._
import fs2.kafka._

final class KafkaEventSender[F[_]: Sync] /* private[infrastructure] */ (
  producer: KafkaProducer[F, String, Event],
  topicPrefix: String
) extends EventSender[F] {
  override def send(event: Event): F[Unit] = event match {
    case departed: Departed =>
      F.unit <* producer
        .produce(
          ProducerRecords.one(
            ProducerRecord(
              s"$topicPrefix-${departed.from.unStation.value}",
              departed.id.unEventId.value,
              departed
            )
          )
        )
    case _ => F.unit
  }
}

object KafkaEventSender {
  def impl[F[_]: Sync](producer: KafkaProducer[F, String, Event], topicPrefix: String) =
    new KafkaEventSender[F](producer, topicPrefix)
}
