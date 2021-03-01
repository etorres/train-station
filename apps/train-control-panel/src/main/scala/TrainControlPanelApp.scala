package es.eriktorr.train_station

import cats.effect.{ExitCode, IO, IOApp}
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

object TrainControlPanelApp extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {
    def program(logger: Logger[IO]): IO[ExitCode] = {
      implicit val _logger: Logger[IO] = logger
      TrainControlPanelServer
        .stream[IO](executionContext)
        .compile
        .drain
        .as(ExitCode.Success)
    }

    for {
      logger <- Slf4jLogger.create[IO]
      result <- program(logger)
    } yield result
  }
}
