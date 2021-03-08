package es.eriktorr.train_station
package messaging.infrastructure

import event.Event
import shared.infrastructure.TrainControlPanelTestConfig

import cats.effect.{ContextShift, IO, Resource, Timer}
import fs2.kafka.{KafkaConsumer, KafkaProducer}

object KafkaTestClient {
  def testKafkaClientResource(
    implicit contextShift: ContextShift[IO],
    timer: Timer[IO]
  ): Resource[IO, (KafkaConsumer[IO, String, Event], KafkaProducer[IO, String, Event])] =
    KafkaClient.clientsFor[IO](
      TrainControlPanelTestConfig.testConfig.kafkaConfig,
      TrainControlPanelTestConfig.testConfig.connectedTo
    )
}
