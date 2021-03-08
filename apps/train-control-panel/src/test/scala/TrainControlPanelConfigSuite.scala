package es.eriktorr.train_station

import shared.infrastructure.TrainControlPanelTestConfig

import cats.effect.IO
import weaver._

object TrainControlPanelConfigSuite extends SimpleIOSuite {
  test("load config params from env vars") {
    TrainControlPanelConfig
      .load[IO]
      .map(actualConfig => expect(actualConfig == TrainControlPanelTestConfig.testConfig))
  }
}
