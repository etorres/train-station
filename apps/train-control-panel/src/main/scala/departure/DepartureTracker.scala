package es.eriktorr.train_station
package departure

import arrival.ExpectedTrains
import arrival.ExpectedTrains.ExpectedTrain
import event.Event.Departed
import station.Station
import station.Station.TravelDirection.{Destination, Origin}

import cats.Applicative
import cats.implicits._
import org.typelevel.log4cats.Logger

trait DepartureTracker[F[_]] {
  def save(event: Departed): F[Unit]
}

object DepartureTracker {
  def impl[F[_]: Applicative: Logger](
    station: Station[Origin],
    expectedTrains: ExpectedTrains[F]
  ): DepartureTracker[F] = (event: Departed) => {
    val updateExpectedTrains =
      expectedTrains.update(ExpectedTrain(event.trainId, event.from, event.expected)) *> F.info(
        s"${station.unStation.value} is expecting ${event.trainId.toString} from ${event.from.toString} at ${event.expected.toString}"
      )

    updateExpectedTrains.whenA(event.to === station.asStation[Destination])
  }
}
