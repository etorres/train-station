package es.eriktorr.train_station
package departure

import arrival.FakeExpectedTrains
import arrival.FakeExpectedTrains.ExpectedTrainsState
import event.Event.Departed
import event.EventId
import station.Station
import station.Station.TravelDirection.{Destination, Origin}
import time.Moment
import time.Moment.When.{Created, Expected}
import train.TrainId

import cats.effect.IO
import cats.implicits._
import fs2.Stream
import fs2.kafka.{ProducerRecord, ProducerRecords}
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import weaver._

import java.time.Instant
import java.util.UUID
import scala.concurrent.duration._

@SuppressWarnings(Array("org.wartremover.warts.OptionPartial"))
object DepartureListenerSuite extends SimpleIOSuite {
  test("track train departures from connected stations") {
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
            case TrainControlPanelContext(_, consumers, producer) =>
              producer.produce(
                ProducerRecords.one(
                  ProducerRecord(
                    "train-departures-Valencia",
                    UUID.randomUUID().toString,
                    Departed(
                      id = EventId.fromString(UUID.randomUUID().toString).toOption.get,
                      trainId = TrainId.fromString("C2").toOption.get,
                      from = Station.fromString[Origin]("Valencia").toOption.get,
                      to = Station.fromString[Destination]("Barcelona").toOption.get,
                      expected = Moment[Expected](Instant.now()),
                      created = Moment[Created](Instant.now())
                    )
                  )
                )
              ) *> Stream
                .emits(consumers.toList)
                .flatMap(_.stream)
                .collect {
                  _.record.value match {
                    case e: Departed => e
                  }
                }
                .evalMap(departureTracker.save)
                .take(1)
                .timeout(30.seconds)
                .compile
                .drain
          }
          departedTrains <- expectedTrainsRef.get
        } yield departedTrains
      }
    } yield expect(departedTrains.trains === List.empty)
  }
}
