package es.eriktorr.train_station

import TrainControlPanelConfig.KafkaConfig
import event.Event
import station.Station
import station.Station.TravelDirection.Destination
import vulcan.EventAvroCodec

import cats.data.NonEmptyList
import cats.effect.{Async, ConcurrentEffect, ContextShift, Resource, Timer}
import cats.implicits._
import fs2.kafka._
import fs2.kafka.vulcan._

final case class TrainControlPanelContext[F[_]](
  config: TrainControlPanelConfig,
  consumer: KafkaConsumer[F, String, Event],
  producer: KafkaProducer[F, String, Event]
)

object TrainControlPanelContext extends EventAvroCodec {
  def impl[F[_]: Async: ContextShift: ConcurrentEffect: Timer]
    : Resource[F, TrainControlPanelContext[F]] = {

    def settingsFrom(kafkaConfig: KafkaConfig) = {
      val avroSettingsSharedClient =
        SchemaRegistryClientSettings[F](kafkaConfig.schemaRegistry.value).createSchemaRegistryClient
          .map(AvroSettings(_))

      avroSettingsSharedClient.map { avroSettings =>
        implicit def eventDeserializer: RecordDeserializer[F, Event] =
          avroDeserializer[Event].using(avroSettings)

        implicit val eventSerializer: RecordSerializer[F, Event] =
          avroSerializer[Event].using(avroSettings)

        val consumerSettings = ConsumerSettings[F, String, Event]
          .withAutoOffsetReset(AutoOffsetReset.Earliest)
          .withBootstrapServers(kafkaConfig.bootstrapServersAsString)
          .withGroupId(kafkaConfig.consumerGroup.value)

        val producerSettings = ProducerSettings[F, String, Event]
          .withBootstrapServers(kafkaConfig.bootstrapServersAsString)

        (consumerSettings, producerSettings)
      }
    }

    def consumer(
      topicPrefix: String,
      consumerSettings: ConsumerSettings[F, String, Event],
      destinations: NonEmptyList[Station[Destination]]
    ) =
      KafkaConsumer
        .resource(consumerSettings)
        .evalTap(
          _.subscribe(
            destinations.map(destination => s"$topicPrefix-${destination.unStation.value}")
          )
        )

    def producer(producerSettings: ProducerSettings[F, String, Event]) =
      KafkaProducer.resource(producerSettings)

    for {
      config <- Resource.liftF(TrainControlPanelConfig.load[F])
      (consumerSettings, producerSettings) <- Resource.liftF(settingsFrom(config.kafkaConfig))
      (consumer, producer) <- (
        consumer(config.kafkaConfig.topic.value, consumerSettings, config.connectedTo),
        producer(producerSettings)
      ).tupled
    } yield TrainControlPanelContext(config, consumer, producer)
  }
}
