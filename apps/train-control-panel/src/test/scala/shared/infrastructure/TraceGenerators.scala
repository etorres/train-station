package es.eriktorr.train_station
package shared.infrastructure

import http.infrastructure.B3Headers

import io.janstenpickle.trace4cats.model.{SpanId, TraceId}
import org.scalacheck._

object TraceGenerators {
  val hexChar: Gen[Char] = Gen.oneOf("0123456789ABCDEF".toCharArray.toIndexedSeq)

  val octChar: Gen[Char] = Gen.oneOf("01234567".toCharArray.toIndexedSeq)

  @SuppressWarnings(Array("org.wartremover.warts.OptionPartial"))
  val spanIdGen: Gen[SpanId] =
    Gen
      .containerOfN[List, Char](16, octChar)
      .map(octal => SpanId.fromHexString(octal.mkString).get)

  @SuppressWarnings(Array("org.wartremover.warts.OptionPartial"))
  val traceIdGen: Gen[TraceId] =
    Gen.containerOfN[List, Char](32, hexChar).map(hex => TraceId.fromHexString(hex.mkString).get)

  val b3Gen: Gen[B3Headers] = for {
    traceId <- traceIdGen
    spanId <- spanIdGen
  } yield B3Headers(traceId, spanId)
}
