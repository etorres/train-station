package es.eriktorr.train_station

import TrainControlPanelConfig.{HttpServerConfig, KafkaConfig}
import effect._
import station.Station
import station.Station.TravelDirection.{Destination, Origin}

import cats.data.NonEmptyList
import cats.effect.{Async, ContextShift}
import cats.implicits._
import ciris._
import ciris.refined._
import eu.timepit.refined.auto._
import eu.timepit.refined.types.net.UserPortNumber
import eu.timepit.refined.types.string.NonEmptyString

final case class TrainControlPanelConfig(
  httpServerConfig: HttpServerConfig,
  kafkaConfig: KafkaConfig,
  station: Station[Origin],
  connectedTo: NonEmptyList[Station[Destination]]
)

object TrainControlPanelConfig {
  final case class HttpServerConfig(host: NonEmptyString, port: UserPortNumber)
  final case class KafkaConfig(
    bootstrapServers: NonEmptyList[NonEmptyString],
    consumerGroup: NonEmptyString,
    topic: NonEmptyString
  ) {
    def bootstrapServersAsString: String = bootstrapServers.toList.mkString(",")
  }

  implicit def nonEmptyListDecoder[A: ConfigDecoder[String, *]]
    : ConfigDecoder[String, NonEmptyList[A]] =
    ConfigDecoder.lift(_.split(",").map(_.trim).toNonEmptyListUnsafe.traverse(A.decode(None, _)))

  implicit def stationDecoder[A <: Station.TravelDirection]: ConfigDecoder[String, Station[A]] =
    ConfigDecoder[String, String].mapEither[Station[A]] { (key, value) =>
      Station.fromString[A](value) match {
        case Left(_) => ConfigError.decode("Station", key, value).asLeft
        case Right(station) => station.asRight
      }
    }

  private def trainControlPanelConfig: ConfigValue[TrainControlPanelConfig] =
    (
      env("HTTP_HOST").as[NonEmptyString].option,
      env("HTTP_PORT").as[UserPortNumber].option,
      env("KAFKA_BOOTSTRAP_SERVERS").as[NonEmptyList[NonEmptyString]].option,
      env("KAFKA_CONSUMER_GROUP").as[NonEmptyString].option,
      env("KAFKA_TOPIC").as[NonEmptyString].option,
      env("STATION").as[Station[Origin]],
      env("CONNECTED_STATIONS").as[NonEmptyList[Station[Destination]]]
    ).parMapN {
      (
        httpHost,
        httpPort,
        kafkaBootstrapServers,
        kafkaConsumerGroup,
        kafkaTopic,
        station,
        connectedStations
      ) =>
        TrainControlPanelConfig(
          HttpServerConfig(httpHost getOrElse "0.0.0.0", httpPort getOrElse 8080),
          KafkaConfig(
            kafkaBootstrapServers getOrElse NonEmptyList.one("localhost:9092"),
            kafkaConsumerGroup getOrElse "train_station",
            kafkaTopic getOrElse "train_arrivals_and_departures"
          ),
          station,
          connectedStations
        )
    }

  def load[F[_]: Async: ContextShift]: F[TrainControlPanelConfig] = trainControlPanelConfig.load[F]
}
