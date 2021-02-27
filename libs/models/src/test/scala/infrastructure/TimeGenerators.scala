package es.eriktorr.train_station
package infrastructure

import org.scalacheck._

import java.time.{Instant, OffsetDateTime}

trait TimeGenerators {
  implicit lazy val instantGen: Arbitrary[Instant] = {
    import java.time.ZoneOffset.UTC
    Arbitrary {
      val now = OffsetDateTime.now(UTC)
      for {
        epochSecond <- Gen.chooseNum(
          now.minusMonths(6L).toEpochSecond,
          now.toEpochSecond
        )
      } yield Instant.ofEpochSecond(epochSecond)
    }
  }
}
