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
  consumers: NonEmptyList[KafkaConsumer[F, String, Event]]
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
      destination: Station[Destination]
    ) =
      KafkaConsumer
        .resource(consumerSettings)
        .evalTap(_.subscribeTo(s"$topicPrefix-${destination.unStation.value}"))

    for {
      config <- Resource.liftF(TrainControlPanelConfig.load[F])
      (consumerSettings, _) <- Resource.liftF(settingsFrom(config.kafkaConfig))
      consumers <- config.connectedTo.traverse(
        consumer(config.kafkaConfig.topic.value, consumerSettings, _)
      )
    } yield TrainControlPanelContext(config, consumers)
  }
}

/*
import cats.effect.concurrent.Ref
import cats.effect.{ Concurrent, ContextShift, Resource }
import cats.implicits._
import cats.{ Inject, Parallel }
import com.psisoyev.train.station.arrival.ExpectedTrains.ExpectedTrain
import cr.pulsar.{ Consumer, Producer, Pulsar, Subscription, Topic, Config => PulsarConfig }
import io.circe.Encoder

final case class Resources[F[_], E](
  config: Config,
  producer: Producer[F, E],
  consumers: List[Consumer[F, E]],
  trainRef: Ref[F, Map[TrainId, ExpectedTrain]]
)

object Resources {
  def make[
    F[_]: Concurrent: ContextShift: Parallel: Logger,
    E: Inject[*, Array[Byte]]: Encoder
  ]: Resource[F, Resources[F, E]] = {
    def topic(config: PulsarConfig, city: City) =
      Topic
        .Builder
        .withName(Topic.Name(city.value.toLowerCase))
        .withConfig(config)
        .withType(Topic.Type.Persistent)
        .build

    def consumer(client: Pulsar.T, config: Config, city: City): Resource[F, Consumer[F, E]] = {
      val name         = s"${city.value}-${config.city.value}"
      val subscription =
        Subscription
          .Builder
          .withName(Subscription.Name(name))
          .withType(Subscription.Type.Failover)
          .build

      val options =
        Consumer
          .Options[F, E]()
          .withLogger(EventLogger.incomingEvents)

      Consumer.withOptions[F, E](client, topic(config.pulsar, city), subscription, options)
    }

    def producer(client: Pulsar.T, config: Config): Resource[F, Producer[F, E]] =
      Producer.withLogger[F, E](client, topic(config.pulsar, config.city), EventLogger.outgoingEvents)

    for {
      config    <- Resource.liftF(Config.load[F])
      client    <- Pulsar.create[F](config.pulsar.url)
      producer  <- producer(client, config)
      consumers <- config.connectedTo.traverse(consumer(client, config, _))
      trainRef  <- Resource.liftF(Ref.of[F, Map[TrainId, ExpectedTrain]](Map.empty))
    } yield Resources(config, producer, consumers, trainRef)
  }
}
 */
