package es.eriktorr.train_station
package messaging.infrastructure

import TrainControlPanelConfig.KafkaConfig
import avro.infrastructure.EventAvroCodec
import event.Event
import station.Station
import station.Station.TravelDirection.Destination

import cats.data.NonEmptyList
import cats.effect._
import cats.implicits._
import fs2.kafka._
import fs2.kafka.vulcan._

object KafkaClient extends EventAvroCodec {
  def clientsFor[F[_]: ConcurrentEffect: ContextShift: Timer](
    kafkaConfig: KafkaConfig,
    connectedTo: NonEmptyList[Station[Destination]]
  ): Resource[F, (KafkaConsumer[F, String, Event], KafkaProducer[F, String, Event])] = {
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
      (consumerSettings, producerSettings) <- Resource.liftF(settingsFrom(kafkaConfig))
      (consumer, producer) <- (
        consumer(kafkaConfig.topic.value, consumerSettings, connectedTo),
        producer(producerSettings)
      ).tupled
    } yield (consumer, producer)
  }
}
