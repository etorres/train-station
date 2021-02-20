package es.eriktorr

import effect._
import error._

import cats.implicits._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.predicates.all._
import eu.timepit.refined.refineV
import io.estatico.newtype.Coercible
import io.estatico.newtype.macros.newtype
import io.estatico.newtype.ops._

object station {
  sealed trait TravelDirection

  sealed trait Origin extends TravelDirection
  sealed trait Destination extends TravelDirection

  @newtype class Station[A <: TravelDirection](val unStation: NonBlankString)

  object Station {
    implicit def evCoercible[A <: TravelDirection, B]: Coercible[B, Station[A]] =
      Coercible.instance[B, Station[A]]

    implicit def evCoercibleRefined[A <: TravelDirection]
      : Coercible[String Refined MatchesRegex[NonBlank], Station[A]] =
      Coercible.instance[String Refined MatchesRegex[NonBlank], Station[A]]

    def fromString[A <: TravelDirection](
      str: String
    ): Either[InvalidParameter, Station[A]] =
      refineV[MatchesRegex[NonBlank]](str) match {
        case Left(_) => InvalidParameter("Station cannot be blank or empty").asLeft
        case Right(refinedStr) => refinedStr.coerce[Station[A]].asRight
      }
  }
}
