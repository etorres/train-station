package es.eriktorr.train_station
package json.infrastructure

import cats.effect.IO
import io.circe.{Decoder, DecodingFailure}
import io.circe.parser.parse
import weaver.SimpleIOSuite

import scala.util.control.NoStackTrace

object StringFieldDecoderSuite extends SimpleIOSuite with StringFieldDecoder {

  final case class TestEntityError() extends NoStackTrace

  final case class TestEntity(value: String)

  object TestEntity {
    def fromString(value: String): Either[TestEntityError, TestEntity] = value match {
      case "ok" => Right(TestEntity(value))
      case _ => Left(TestEntityError())
    }
    def right(value: String) = Right(new TestEntity(value))
  }

  test("should decode nested values") {
    for {
      implicit0(testEntityDecoder: Decoder[TestEntity]) <- IO(
        decode[TestEntity]("testEntity", TestEntity.fromString)
      )
      json <- IO.fromEither(parse("{\"testEntity\":\"ok\"}"))
      result = json.as[TestEntity]
    } yield expect(result == TestEntity.right("ok"))
  }

  test("should decode values") {
    for {
      implicit0(testEntityDecoder: Decoder[TestEntity]) <- IO(
        decodeValue[TestEntity](TestEntity.fromString)
      )
      json <- IO.fromEither(parse("\"ok\""))
      result = json.as[TestEntity]
    } yield expect(result == TestEntity.right("ok"))
  }

  test("should fail with decoding failure when parsing illegal values") {
    for {
      implicit0(testEntityDecoder: Decoder[TestEntity]) <- IO(
        decodeValue[TestEntity](TestEntity.fromString)
      )
      json <- IO.fromEither(parse("\"wrong\""))
      result = json.as[TestEntity]
    } yield expect(result == Left(DecodingFailure.fromThrowable(TestEntityError(), List.empty)))
  }
}
