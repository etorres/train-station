package es.eriktorr

import cats.effect.{ExitCode, IO, IOApp}

object TrainControlPanelApp extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    TrainControlPanelServer.stream[IO](executionContext).compile.drain.as(ExitCode.Success)
}
