package es.eriktorr.train_station
package avro.infrastructure

import time.Moment

import vulcan._

import java.time.ZoneOffset

trait MomentAvroCodec {
  implicit def momentCodec[A <: Moment.When]: Codec[Moment[A]] =
    Codec.instant.imap(instant => Moment[A](instant.atOffset(ZoneOffset.UTC)))(_.unMoment.toInstant)
}
