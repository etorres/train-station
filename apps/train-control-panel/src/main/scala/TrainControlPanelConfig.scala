package es.eriktorr.train_station

import TrainControlPanelConfig.HttpServerConfig
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
  station: Station[Origin],
  connectedTo: NonEmptyList[Station[Destination]]
)

object TrainControlPanelConfig {
  final case class HttpServerConfig(host: NonEmptyString, port: UserPortNumber)

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
      env("STATION").as[Station[Origin]],
      env("CONNECTED_STATIONS").as[NonEmptyList[Station[Destination]]]
    ).parMapN { (httpHost, httpPort, station, connectedStations) =>
      TrainControlPanelConfig(
        HttpServerConfig(httpHost getOrElse "0.0.0.0", httpPort getOrElse 8080),
        station,
        connectedStations
      )
    }

  def load[F[_]: Async: ContextShift]: F[TrainControlPanelConfig] = trainControlPanelConfig.load[F]
}
