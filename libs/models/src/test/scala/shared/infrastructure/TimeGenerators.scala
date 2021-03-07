package es.eriktorr.train_station
package shared.infrastructure

import org.scalacheck._

import java.time.{Instant, OffsetDateTime}

trait TimeGenerators {
  implicit lazy val offsetDateTimeGen: Arbitrary[OffsetDateTime] = {
    import java.time.ZoneOffset.UTC
    Arbitrary {
      val now = OffsetDateTime.now(UTC)
      for {
        epochSecond <- Gen.chooseNum(
          now.minusMonths(6L).toEpochSecond,
          now.toEpochSecond
        )
      } yield OffsetDateTime.ofInstant(Instant.ofEpochSecond(epochSecond), UTC)
    }
  }
}
