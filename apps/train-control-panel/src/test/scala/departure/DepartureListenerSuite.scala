package es.eriktorr.train_station
package departure

import arrival.FakeExpectedTrains
import arrival.FakeExpectedTrains.ExpectedTrainsState
import event.Event.Departed
import station.Station
import station.Station.TravelDirection.Origin

import cats.effect.IO
import fs2.Stream
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import weaver._

object DepartureListenerSuite extends SimpleIOSuite {
  test("work") {
    for {
      logger <- Slf4jLogger.create[IO]
      departedTrains <- {
        implicit val testLogger: Logger[IO] = logger
        for {
          expectedTrainsRef <- ExpectedTrainsState.refFrom(List.empty)
          station <- IO.fromEither(Station.fromString[Origin]("Barcelona"))
          expectedTrains = FakeExpectedTrains.impl[IO](expectedTrainsRef)
          departureTracker = DepartureTracker.impl[IO](station, expectedTrains)
          _ <- TrainControlPanelContext.impl[IO].use {
            case TrainControlPanelContext(_, consumers) =>
              Stream
                .emits(consumers.toList)
                .flatMap(_.stream)
                .collect {
                  _.record.value match {
                    case e: Departed => e
                  }
                }
                .evalMap(departureTracker.save)
                .compile
                .drain
          }
          departedTrains <- expectedTrainsRef.get
        } yield departedTrains
      }
    } yield expect(departedTrains.trains == List.empty)
  }
}
