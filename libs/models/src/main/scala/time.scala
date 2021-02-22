package es.eriktorr

import io.estatico.newtype.macros.newtype

import java.time.Instant

object time {
  @newtype case class Moment[A <: Moment.When](unMoment: Instant)

  object Moment {
    sealed trait When

    object When {
      sealed trait Actual extends When
      sealed trait Created extends When
      sealed trait Expected extends When
    }
  }
}
