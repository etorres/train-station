package es.eriktorr.train_station
package shared.infrastructure

import arrival.ExpectedTrains.ExpectedTrain
import shared.infrastructure.TrainStationGenerators.momentGen
import station.Station
import station.Station.TravelDirection.Origin
import time.Moment.When.Expected
import train.TrainId

import org.scalacheck._

object TrainControlPanelGenerators {
  def expectedTrainGen(trainId: TrainId, originGen: Gen[Station[Origin]]): Gen[ExpectedTrain] =
    for {
      origin <- originGen
      expected <- momentGen[Expected]
    } yield ExpectedTrain(trainId, origin, expected)
}
