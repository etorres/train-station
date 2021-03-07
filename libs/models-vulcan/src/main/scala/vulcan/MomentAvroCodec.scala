package es.eriktorr.train_station
package vulcan

import time.Moment

import _root_.vulcan._

import java.time.ZoneOffset

trait MomentAvroCodec {
  implicit def momentCodec[A <: Moment.When]: Codec[Moment[A]] =
    Codec.instant.imap(instant => Moment[A](instant.atOffset(ZoneOffset.UTC)))(_.unMoment.toInstant)
}
