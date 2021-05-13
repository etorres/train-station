package es.eriktorr.train_station

import TrainControlPanelConfig.{HttpServerConfig, JdbcConfig, KafkaConfig}
import effect._
import station.Station
import station.Station.TravelDirection.{Destination, Origin}

import cats.data.NonEmptyList
import cats.effect.Async
import cats.implicits._
import ciris._
import ciris.refined._
import eu.timepit.refined.auto._
import eu.timepit.refined.cats._
import eu.timepit.refined.types.net.UserPortNumber
import eu.timepit.refined.types.string.NonEmptyString

final case class TrainControlPanelConfig(
  httpServerConfig: HttpServerConfig,
  jdbcConfig: JdbcConfig,
  kafkaConfig: KafkaConfig,
  station: Station[Origin],
  connectedTo: NonEmptyList[Station[Destination]]
)

object TrainControlPanelConfig {
  final case class HttpServerConfig(host: NonEmptyString, port: UserPortNumber)

  final case class JdbcConfig(
    driverClassName: NonEmptyString,
    connectUrl: NonEmptyString,
    user: NonEmptyString,
    password: Secret[NonEmptyString]
  )

  final case class KafkaConfig(
    bootstrapServers: NonEmptyList[NonEmptyString],
    consumerGroup: NonEmptyString,
    topic: NonEmptyString,
    schemaRegistry: NonEmptyString
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
      env("JDBC_DRIVER_CLASS_NAME").as[NonEmptyString].option,
      env("JDBC_CONNECT_URL").as[NonEmptyString].option,
      env("JDBC_USER").as[NonEmptyString].option,
      env("JDBC_PASSWORD").as[NonEmptyString].secret,
      env("KAFKA_BOOTSTRAP_SERVERS").as[NonEmptyList[NonEmptyString]].option,
      env("KAFKA_CONSUMER_GROUP").as[NonEmptyString].option,
      env("KAFKA_TOPIC").as[NonEmptyString].option,
      env("KAFKA_SCHEMA_REGISTRY").as[NonEmptyString].option,
      env("STATION").as[Station[Origin]],
      env("CONNECTED_STATIONS").as[NonEmptyList[Station[Destination]]]
    ).parMapN {
      (
        httpHost,
        httpPort,
        jdbcDriverClassName,
        jdbcConnectUrl,
        jdbcUser,
        jdbcPassword,
        kafkaBootstrapServers,
        kafkaConsumerGroup,
        kafkaTopic,
        kafkaSchemaRegistry,
        station,
        connectedStations
      ) =>
        TrainControlPanelConfig(
          HttpServerConfig(httpHost getOrElse "0.0.0.0", httpPort getOrElse 8080),
          JdbcConfig(
            jdbcDriverClassName getOrElse "org.postgresql.Driver",
            jdbcConnectUrl getOrElse "jdbc:postgresql://localhost:5432/train_station",
            jdbcUser getOrElse "train_station",
            jdbcPassword
          ),
          KafkaConfig(
            kafkaBootstrapServers getOrElse NonEmptyList.one("localhost:29092"),
            kafkaConsumerGroup getOrElse "train-station",
            kafkaTopic getOrElse "train-arrivals-and-departures",
            kafkaSchemaRegistry getOrElse "http://localhost:8081/api/ccompat"
          ),
          station,
          connectedStations
        )
    }

  def load[F[_]: Async: ContextShift]: F[TrainControlPanelConfig] = trainControlPanelConfig.load[F]
}
