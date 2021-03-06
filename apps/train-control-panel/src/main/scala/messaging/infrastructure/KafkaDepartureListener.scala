package es.eriktorr.train_station
package messaging.infrastructure

import departure.DepartureTracker
import event.Event
import event.Event.Departed

import cats.effect._
import cats.implicits._
import fs2.Stream
import fs2.kafka._

import scala.concurrent.duration._

object KafkaDepartureListener {
  def stream[F[_]: Concurrent: Timer](
    consumer: KafkaConsumer[F, String, Event],
    departureTracker: DepartureTracker[F]
  ): Stream[F, Unit] =
    consumer.stream
      .mapAsync(16) { committable =>
        (committable.record.value match {
          case e: Departed => departureTracker.save(e)
          case _ => F.unit
        }).as(committable.offset)
      }
      .through(commitBatchWithin(500, 15.seconds))
}
