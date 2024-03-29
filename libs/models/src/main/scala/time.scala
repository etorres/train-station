package es.eriktorr.train_station

import cats.{Eq, Show}
import io.estatico.newtype.Coercible
import io.estatico.newtype.macros.newtype
import io.estatico.newtype.ops._

import java.time.OffsetDateTime

object time {
  @newtype case class Moment[A <: Moment.When](unMoment: OffsetDateTime) {
    def as[B <: Moment.When]: Moment[B] = unMoment.coerce[Moment[B]]
  }

  implicit def evCoercible[A <: Moment.When, B]: Coercible[B, Moment[A]] =
    Coercible.instance[B, Moment[A]]

  object Moment {
    sealed trait When

    object When {
      sealed trait Actual extends When
      sealed trait Created extends When
      sealed trait Expected extends When
    }
  }

  implicit def eqMoment[A <: Moment.When]: Eq[Moment[A]] = Eq.fromUniversalEquals
  implicit def showMoment[A <: Moment.When]: Show[Moment[A]] = Show.fromToString
}
