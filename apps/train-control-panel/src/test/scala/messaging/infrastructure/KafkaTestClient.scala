package es.eriktorr.train_station
package messaging.infrastructure

import event.Event
import shared.infrastructure.TrainControlPanelTestConfig

import cats.effect.{IO, Resource}
import fs2.kafka.{KafkaConsumer, KafkaProducer}
import cats.effect.Temporal

object KafkaTestClient {
  def testKafkaClientResource(implicit
    timer: Temporal[IO]
  ): Resource[IO, (KafkaConsumer[IO, String, Event], KafkaProducer[IO, String, Event])] =
    KafkaClient.clientsFor[IO](
      TrainControlPanelTestConfig.testConfig.kafkaConfig,
      TrainControlPanelTestConfig.testConfig.connectedTo
    )
}
