package es.eriktorr

import io.estatico.newtype.macros.newtype

import java.time.Instant

object time {
  sealed trait Record

  sealed trait Created extends Record
  sealed trait Expected extends Record

  @newtype case class Moment[A <: Record](unTimestamp: Instant)
}
