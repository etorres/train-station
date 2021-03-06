package es.eriktorr.train_station
package departure

import arrival.ExpectedTrains.ExpectedTrain
import arrival.infrastructure.FakeExpectedTrains
import arrival.infrastructure.FakeExpectedTrains.ExpectedTrainsState
import event.Event.Departed
import shared.infrastructure.GeneratorSyntax._
import shared.infrastructure.TrainStationGenerators.{afterGen, eventIdGen, momentGen, trainIdGen}
import station.Station
import station.Station.TravelDirection.{Destination, Origin}
import time.Moment.When.{Created, Expected}

import cats.effect.IO
import cats.implicits._
import fs2.kafka.{commitBatchWithin, ProducerRecord, ProducerRecords}
import org.scalacheck.Gen
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import weaver._
import weaver.scalacheck._

import scala.concurrent.duration._

@SuppressWarnings(Array("org.wartremover.warts.OptionPartial"))
object DepartureListenerSuite extends SimpleIOSuite with Checkers {
  test("track train departures from connected stations") {

    val (origin, destination, eventId, trainId, expected, created) = (for {
      origin <- Gen
        .oneOf("Madrid", "Valencia")
        .map(Station.fromString[Origin])
        .map(_.toOption.get)
      destination <- Gen.const("Barcelona").map(Station.fromString[Destination]).map(_.toOption.get)
      eventId <- eventIdGen
      trainId <- trainIdGen
      created <- momentGen[Created]
      expected <- afterGen(created).map(_.asMoment[Expected])
    } yield (origin, destination, eventId, trainId, expected, created))
      .sampleWithSeed("DepartureListenerSuite")

    for {
      logger <- Slf4jLogger.create[IO]
      departedTrains <- {
        implicit val testLogger: Logger[IO] = logger
        for {
          expectedTrainsRef <- ExpectedTrainsState.refFrom(List.empty)
          expectedTrains = FakeExpectedTrains.impl[IO](expectedTrainsRef)
          departureTracker = DepartureTracker.impl[IO](destination, expectedTrains)
          _ <- TrainControlPanelContext.impl[IO].use {
            case TrainControlPanelContext(_, consumer, producer) =>
              producer.produce(
                ProducerRecords.one(
                  ProducerRecord(
                    s"train-arrivals-and-departures-${origin.unStation.value}",
                    eventId.unEventId.value,
                    Departed(eventId, trainId, origin, destination, expected, created)
                  )
                )
              ) *> consumer.stream
                .mapAsync(16) { committable =>
                  (committable.record.value match {
                    case e: Departed => departureTracker.save(e)
                    case _ => IO.unit
                  }).as(committable.offset)
                }
                .through(commitBatchWithin(500, 15.seconds))
                .timeout(30.seconds)
                .take(1)
                .compile
                .drain
          }
          departedTrains <- expectedTrainsRef.get
        } yield departedTrains
      }
    } yield expect(departedTrains.trains === List(ExpectedTrain(trainId, origin, expected)))
  }
}
