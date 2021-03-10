package es.eriktorr.train_station

import eu.timepit.refined._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.predicates.all._
import io.estatico.newtype.Coercible

object refined {
  type NonBlank = W.`"""\\A(?!\\s*\\Z).+"""`.T

  type NonBlankString = String Refined NonBlank

  type InUserPortRange = Not[Less[1024]] And Not[Greater[49151]]

  implicit def evNonBlankString[A]: Coercible[String Refined MatchesRegex[NonBlank], A] =
    Coercible.instance[String Refined MatchesRegex[NonBlank], A]

  implicit def evUuidString[A]: Coercible[String Refined Uuid, A] =
    Coercible.instance[String Refined Uuid, A]
}
