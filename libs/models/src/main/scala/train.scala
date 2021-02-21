package es.eriktorr

import effect._
import error.TrainStationError.InvalidParameter

import cats.implicits._
import eu.timepit.refined.predicates.all.Uuid
import eu.timepit.refined.refineV
import io.estatico.newtype.macros.newtype
import io.estatico.newtype.ops._

object train {
  @newtype class TrainId(val unTrainId: Uuid)

  object TrainId {
    def fromString(str: String): Either[InvalidParameter, TrainId] = refineV[Uuid](str) match {
      case Left(_) => InvalidParameter("Train Id should be a valid UUID").asLeft
      case Right(refinedStr) => refinedStr.coerce[TrainId].asRight
    }
  }
}
