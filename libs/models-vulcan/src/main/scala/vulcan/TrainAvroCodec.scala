package es.eriktorr.train_station
package vulcan

import train.TrainId

import _root_.vulcan._
import cats.implicits._

trait TrainAvroCodec {
  implicit val trainIdCodec: Codec[TrainId] = Codec.string.imapError(value =>
    TrainId.fromString(value) match {
      case Left(constructorError) => AvroError(constructorError.error).asLeft
      case Right(trainId) => trainId.asRight
    }
  )(_.unTrainId.value)
}
