package es.eriktorr.train_station
package circe

import time.Moment

import io.circe._

import java.time.OffsetDateTime
import scala.util.Try

trait MomentJsonProtocol extends StringFieldDecoder {
  implicit def momentDecoder[A <: Moment.When]: Decoder[Moment[A]] =
    decode[Moment[A]]("moment", str => Try(OffsetDateTime.parse(str)).map(Moment[A]).toEither)

  implicit def momentEncoder[A <: Moment.When]: Encoder[Moment[A]] =
    (moment: Moment[A]) => Json.obj(("moment", Json.fromString(moment.unMoment.toString)))
}
