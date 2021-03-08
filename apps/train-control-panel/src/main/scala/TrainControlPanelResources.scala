package es.eriktorr.train_station

import avro.infrastructure.EventAvroCodec
import event.Event
import jdbc.infrastructure.JdbcTransactor
import messaging.infrastructure.KafkaClient

import cats.effect._
import doobie.Transactor
import fs2.kafka._

import scala.concurrent.ExecutionContext

final case class TrainControlPanelResources[F[_]](
  config: TrainControlPanelConfig,
  consumer: KafkaConsumer[F, String, Event],
  producer: KafkaProducer[F, String, Event],
  transactor: Transactor[F]
)

object TrainControlPanelResources extends EventAvroCodec {
  def impl[F[_]: Async: ConcurrentEffect](
    implicit executionContext: ExecutionContext,
    contextShift: ContextShift[F],
    blocker: Blocker,
    timer: Timer[F]
  ): Resource[F, TrainControlPanelResources[F]] =
    for {
      config <- Resource.liftF(TrainControlPanelConfig.load[F])
      (consumer, producer) <- KafkaClient.clientsFor(config.kafkaConfig, config.connectedTo)
      transactor <- JdbcTransactor.impl[F](config.jdbcConfig).transactorResource
    } yield TrainControlPanelResources(config, consumer, producer, transactor)
}
