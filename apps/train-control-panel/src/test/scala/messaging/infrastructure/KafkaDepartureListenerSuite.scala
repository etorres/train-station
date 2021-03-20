package es.eriktorr.train_station
package messaging.infrastructure

import arrival.ExpectedTrains.ExpectedTrain
import arrival.infrastructure.FakeExpectedTrains
import arrival.infrastructure.FakeExpectedTrains.ExpectedTrainsState
import departure.DepartureTracker
import event.Event.Departed
import shared.infrastructure.GeneratorSyntax._
import shared.infrastructure.TrainControlPanelTestConfig
import shared.infrastructure.TrainStationGenerators.{afterGen, eventIdGen, momentGen, trainIdGen}
import station.Station
import station.Station.TravelDirection.{Destination, Origin}
import time.Moment.When.{Created, Expected}

import cats.effect.{IO, Resource}
import cats.implicits._
import org.scalacheck.Gen
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import weaver._
import weaver.scalacheck._

import scala.concurrent.duration._

@SuppressWarnings(Array("org.wartremover.warts.OptionPartial"))
object KafkaDepartureListenerSuite extends SimpleIOSuite with Checkers {
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
          _ <- (
            KafkaTestClient.testKafkaClientResource,
            Resource.liftF(IO.pure(TrainControlPanelTestConfig.testConfig))
          ).tupled.use { case ((consumer, producer), config) =>
            KafkaEventSender
              .impl[IO](producer, config.kafkaConfig.topic)
              .send(
                Departed(eventId, trainId, origin, destination, expected, created)
              ) *> KafkaDepartureListener
              .stream[IO](consumer, departureTracker)
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
