package es.eriktorr.train_station
package doobie

import time.Moment

import _root_.doobie._
import _root_.doobie.implicits.javatime._

import java.time.OffsetDateTime

trait TimeMapping {
  implicit def momentPut[A <: Moment.When]: Put[Moment[A]] =
    Put[OffsetDateTime].contramap(_.unMoment)

  implicit def momentRead[A <: Moment.When]: Read[Moment[A]] = Read[OffsetDateTime].map(Moment[A])
}
