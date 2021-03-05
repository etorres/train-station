package es.eriktorr.train_station
package departure

import arrival.ExpectedTrains.ExpectedTrain
import arrival.FakeExpectedTrains
import arrival.FakeExpectedTrains.ExpectedTrainsState
import event.Event.Departed
import event.EventId
import shared.infrastructure.GeneratorSyntax._
import shared.infrastructure.TrainStationGenerators.{afterGen, eventIdGen, momentGen, trainIdGen}
import station.Station
import station.Station.TravelDirection.{Destination, Origin}
import time.Moment
import time.Moment.When.{Created, Expected}
import train.TrainId

import cats.Show
import cats.derived._
import cats.effect.IO
import cats.implicits._
import fs2.Stream
import fs2.kafka.{ProducerRecord, ProducerRecords}
import org.scalacheck.Gen
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import weaver._
import weaver.scalacheck._

import scala.concurrent.duration._

@SuppressWarnings(Array("org.wartremover.warts.OptionPartial"))
object DepartureListenerSuite extends SimpleIOSuite with Checkers {
  test("track train departures from connected stations") {
    final case class TestCase(
      origin: Station[Origin],
      destination: Station[Destination],
      eventId: EventId,
      trainId: TrainId,
      expected: Moment[Expected],
      created: Moment[Created]
    )

    object TestCase {
      implicit val showTestCase: Show[TestCase] = semiauto.show
    }

    val gen = (for {
      origin <- Gen.const("Barcelona").map(Station.fromString[Origin]).map(_.toOption.get)
      destination <- Gen
        .oneOf("Madrid", "Valencia")
        .map(Station.fromString[Destination])
        .map(_.toOption.get)
      eventId <- eventIdGen
      trainId <- trainIdGen
      created <- momentGen[Created]
      expected <- afterGen(created).map(_.asMoment[Expected])
    } yield TestCase(origin, destination, eventId, trainId, expected, created))
      .sampleWithSeed("DepartureListenerSuite")

    forall(gen) {
      case TestCase(origin, destination, eventId, trainId, expected, created) =>
        for {
          logger <- Slf4jLogger.create[IO]
          departedTrains <- {
            implicit val testLogger: Logger[IO] = logger
            for {
              expectedTrainsRef <- ExpectedTrainsState.refFrom(List.empty)
              expectedTrains = FakeExpectedTrains.impl[IO](expectedTrainsRef)
              departureTracker = DepartureTracker.impl[IO](origin, expectedTrains)
              _ <- TrainControlPanelContext.impl[IO].use {
                case TrainControlPanelContext(_, consumers, producer) =>
                  (
                    Stream
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
                      .drain,
                    producer.produce(
                      ProducerRecords.one(
                        ProducerRecord(
                          s"train-departures-${destination.unStation.value}",
                          eventId.unEventId.value,
                          Departed(eventId, trainId, origin, destination, expected, created)
                        )
                      )
                    )
                  ).parMapN((_, _) => ())
              }
              departedTrains <- expectedTrainsRef.get
            } yield departedTrains
          }
        } yield expect(departedTrains.trains === List(ExpectedTrain(trainId, origin, expected)))
    }
  }
}
