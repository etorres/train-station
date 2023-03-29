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
import com.comcast.ip4s.{Host, IpLiteralSyntax, Port}
import eu.timepit.refined.auto._
import eu.timepit.refined.cats._
import eu.timepit.refined.types.string.NonEmptyString

final case class TrainControlPanelConfig(
  httpServerConfig: HttpServerConfig,
  jdbcConfig: JdbcConfig,
  kafkaConfig: KafkaConfig,
  station: Station[Origin],
  connectedTo: NonEmptyList[Station[Destination]]
)

object TrainControlPanelConfig {
  final case class HttpServerConfig(host: Host, port: Port)

  @SuppressWarnings(Array("org.wartremover.warts.OptionPartial"))
  object HttpServerConfig {
    val defaultHost: Host = host"0.0.0.0"
    val defaultPort: Port = port"8080"

    val default: HttpServerConfig = HttpServerConfig(defaultHost, defaultPort)
  }

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

  implicit def hostDecoder: ConfigDecoder[String, Host] = ConfigDecoder.lift { host =>
    Host.fromString(host) match {
      case Some(value) => Right(value)
      case None => Left(ConfigError("Invalid host"))
    }
  }
  implicit def portDecoder: ConfigDecoder[String, Port] = ConfigDecoder.lift { port =>
    Port.fromString(port) match {
      case Some(value) => Right(value)
      case None => Left(ConfigError("Invalid port"))
    }
  }

  private def trainControlPanelConfig: ConfigValue[Effect, TrainControlPanelConfig] =
    (
      env("HTTP_HOST").as[Host].option,
      env("HTTP_PORT").as[Port].option,
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
          HttpServerConfig(
            httpHost.fold(HttpServerConfig.defaultHost)(identity),
            httpPort.fold(HttpServerConfig.defaultPort)(identity)
          ),
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
            kafkaSchemaRegistry getOrElse "http://localhost:8081"
          ),
          station,
          connectedStations
        )
    }

  def load[F[_]: Async]: F[TrainControlPanelConfig] = trainControlPanelConfig.load[F]
}
