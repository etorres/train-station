package es.eriktorr.train_station

import arrival.Arrivals
import arrival.infrastructure.JdbcExpectedTrains
import departure.{DepartureTracker, Departures}
import http.infrastructure.HttpServer
import messaging.infrastructure.{KafkaDepartureListener, KafkaEventSender}
import station.Station.TravelDirection.Destination

import cats.effect.{Blocker, ExitCode, IO, IOApp}
import cats.implicits._
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.concurrent.ExecutionContext

object TrainControlPanelApp extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {
    def program(
      implicit ec: ExecutionContext,
      _blocker: Blocker,
      logger: Logger[IO]
    ): IO[ExitCode] =
      TrainControlPanelResources.impl[IO].use {
        case TrainControlPanelResources(config, consumer, producer, transactor) =>
          val expectedTrains = JdbcExpectedTrains.impl[IO](transactor)
          val departureTracker =
            DepartureTracker.impl[IO](config.station.asStation[Destination], expectedTrains)

          val departureListener =
            KafkaDepartureListener.stream[IO](consumer, departureTracker).compile.drain

          val eventSender = KafkaEventSender.impl[IO](producer, config.kafkaConfig.topic.value)
          val arrivals =
            Arrivals.impl[IO](config.station.asStation[Destination], expectedTrains, eventSender)
          val departures = Departures.impl[IO](config.station, config.connectedTo, eventSender)

          val httpServer = HttpServer
            .stream[IO](arrivals, departures, executionContext, config.httpServerConfig)
            .compile
            .drain

          logger.info(s"Started train station ${config.station.toString}") *> (
            departureListener,
            httpServer
          ).parMapN((_, _) => ()).as(ExitCode.Success)
      }

    for {
      logger <- Slf4jLogger.create[IO]
      result <- {
        implicit val _executionContext: ExecutionContext = executionContext
        implicit val _blocker: Blocker = Blocker.liftExecutionContext(_executionContext)
        implicit val _logger: Logger[IO] = logger
        program
      }
    } yield result
  }
}
