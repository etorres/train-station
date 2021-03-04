package es.eriktorr.train_station
package vulcan

import time.Moment

import _root_.vulcan._

trait MomentAvroCodec {
  implicit def momentCodec[A <: Moment.When]: Codec[Moment[A]] =
    Codec.instant.imap(Moment[A])(_.unMoment)
}
