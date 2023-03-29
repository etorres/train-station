package es.eriktorr.train_station
package shared.infrastructure

import TrainControlPanelConfig.{HttpServerConfig, JdbcConfig, KafkaConfig}
import station.Station
import station.Station.TravelDirection.{Destination, Origin}

import cats.data.NonEmptyList
import ciris.Secret
import eu.timepit.refined._
import eu.timepit.refined.cats._
import eu.timepit.refined.predicates.all._

object TrainControlPanelTestConfig {
  @SuppressWarnings(Array("org.wartremover.warts.OptionPartial"))
  val testConfig: TrainControlPanelConfig = TrainControlPanelConfig(
    HttpServerConfig.default,
    JdbcConfig(
      refineMV[NonEmpty]("org.postgresql.Driver"),
      refineMV[NonEmpty]("jdbc:postgresql://localhost:5432/train_station"),
      refineMV[NonEmpty]("train_station"),
      Secret(refineMV[NonEmpty]("changeme"))
    ),
    KafkaConfig(
      NonEmptyList.one(refineMV[NonEmpty]("localhost:29092")),
      refineMV[NonEmpty]("train-station"),
      refineMV[NonEmpty]("train-arrivals-and-departures"),
      refineMV[NonEmpty]("http://localhost:8081")
    ),
    Station.fromString[Origin]("Barcelona").toOption.get,
    NonEmptyList.fromListUnsafe(
      List(
        Station.fromString[Destination]("Madrid").toOption.get,
        Station.fromString[Destination]("Valencia").toOption.get
      )
    )
  )
}
