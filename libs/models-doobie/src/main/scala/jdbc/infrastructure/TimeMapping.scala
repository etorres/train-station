package es.eriktorr.train_station
package jdbc.infrastructure

import time.Moment

import doobie._
import doobie.postgres.implicits._

import java.time.OffsetDateTime

trait TimeMapping {
  implicit def momentPut[A <: Moment.When]: Put[Moment[A]] =
    Put[OffsetDateTime].contramap(_.unMoment)

  implicit def momentRead[A <: Moment.When]: Read[Moment[A]] = Read[OffsetDateTime].map(Moment[A])
}
