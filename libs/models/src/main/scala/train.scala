package es.eriktorr.train_station

import error.TrainStationError.InvalidParameter
import refined._

import cats.implicits._
import cats.{Eq, Show}
import eu.timepit.refined.predicates.all.MatchesRegex
import eu.timepit.refined.refineV
import io.estatico.newtype.macros.newtype
import io.estatico.newtype.ops._

object train {
  @newtype class TrainId(val unTrainId: NonBlankString)

  object TrainId {
    def fromString(str: String): Either[InvalidParameter, TrainId] =
      refineV[MatchesRegex[NonBlank]](str) match {
        case Left(_) => InvalidParameter("Train Id should be a valid UUID").asLeft
        case Right(refinedStr) => refinedStr.coerce[TrainId].asRight
      }

    implicit val eqTrainId: Eq[TrainId] = Eq.fromUniversalEquals
    implicit val showTrainId: Show[TrainId] = Show.fromToString
  }
}
