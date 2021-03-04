package es.eriktorr.train_station

import arrival.ExpectedTrains
import departure.DepartureTracker
import event.Event
import event.Event.Departed
import vulcan.EventAvroCodec

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import fs2.kafka._
import fs2.kafka.vulcan.{avroDeserializer, AvroSettings, SchemaRegistryClientSettings}
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

object TrainControlPanelApp extends IOApp with EventAvroCodec {
  override def run(args: List[String]): IO[ExitCode] = {
    def program(config: TrainControlPanelConfig, logger: Logger[IO]): IO[ExitCode] = {
      implicit val _logger: Logger[IO] = logger

      val expectedTrains = ExpectedTrains.impl[IO]
      val departureTracker = DepartureTracker.impl[IO](config.station, expectedTrains)

      val avroSettings =
        AvroSettings {
          SchemaRegistryClientSettings[IO]("http://localhost:8081")
        }

      implicit val eventDeserializer: RecordDeserializer[IO, Event] =
        avroDeserializer[Event].using(avroSettings)

      val consumerSettings =
        ConsumerSettings[IO, String, Event]
          .withAutoOffsetReset(AutoOffsetReset.Earliest)
          .withBootstrapServers(config.kafkaConfig.bootstrapServersAsString)
          .withGroupId(config.kafkaConfig.consumerGroup.value)

      val departureListener = KafkaConsumer
        .stream(consumerSettings)
        .evalTap(_.subscribeTo(config.kafkaConfig.topic.value))
        .flatMap(_.stream)
        .collect {
          _.record.value match {
            case e: Departed => e
          }
        }
        .evalMap(departureTracker.save)
        .compile
        .drain

      val httpServer = TrainControlPanelServer
        .stream[IO](executionContext)
        .compile
        .drain

      logger.info(s"Started train station ${config.station.toString}") *> (
        departureListener,
        httpServer
      ).parMapN((_, _) => ())
        .as(ExitCode.Success)
    }

    // TODO: https://fd4s.github.io/fs2-kafka/docs/consumers#graceful-shutdown

    /*
    val producerSettings = ProducerSettings[IO, String, Event]
        .withBootstrapServers(config.kafkaConfig.bootstrapServersAsString)

    def program2(logger: Logger[IO]): IO[ExitCode] = {
      implicit val _logger: Logger[IO] = logger
      TrainControlPanelServer
        .stream[IO](executionContext)
        .compile
        .drain
        .as(ExitCode.Success)
    }
     */

    for {
      logger <- Slf4jLogger.create[IO]
      config <- TrainControlPanelConfig.load[IO]
      result <- program(config, logger)
    } yield result
  }
}
