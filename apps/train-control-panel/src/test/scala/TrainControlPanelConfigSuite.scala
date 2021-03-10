package es.eriktorr.train_station

import shared.infrastructure.TrainControlPanelTestConfig

import cats.effect.IO
import cats.implicits._
import weaver._

object TrainControlPanelConfigSuite extends SimpleIOSuite {
  test("load config params from env vars") {
    for {
      onCI <- IO(sys.env.contains("CI"))
      _ <- ignore("not on CI").unlessA(onCI)
      actualConfig <- TrainControlPanelConfig.load[IO]
    } yield expect(actualConfig == TrainControlPanelTestConfig.testConfig)
  }
}
