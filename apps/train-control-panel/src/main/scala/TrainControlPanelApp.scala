package es.eriktorr.train_station

import arrival.ExpectedTrains
import departure.DepartureTracker
import event.Event.Departed
import http_server.infrastructure.HttpServer
import station.Station.TravelDirection.Destination

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import fs2.kafka.commitBatchWithin
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.concurrent.duration.DurationInt

object TrainControlPanelApp extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {
    def program(config: TrainControlPanelConfig, logger: Logger[IO]): IO[ExitCode] = {
      implicit val _logger: Logger[IO] = logger

      val expectedTrains = ExpectedTrains.impl[IO]
      val departureTracker =
        DepartureTracker.impl[IO](config.station.asStation[Destination], expectedTrains)

      TrainControlPanelContext.impl[IO].use {
        case TrainControlPanelContext(_, consumer, _) =>
          val departureListener = consumer.stream
            .mapAsync(16) { committable =>
              (committable.record.value match {
                case e: Departed => departureTracker.save(e)
                case _ => IO.unit
              }).as(committable.offset)
            }
            .through(commitBatchWithin(500, 15.seconds))
            .compile
            .drain

          val httpServer = HttpServer
            .stream[IO](executionContext)
            .compile
            .drain

          logger.info(s"Started train station ${config.station.toString}") *> (
            departureListener,
            httpServer
          ).parMapN((_, _) => ())
            .as(ExitCode.Success)
      }
    }

    for {
      logger <- Slf4jLogger.create[IO]
      config <- TrainControlPanelConfig.load[IO]
      result <- program(config, logger)
    } yield result
  }
}
