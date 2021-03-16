package es.eriktorr.train_station
package uuid.infrastructure

import uuid.UUIDGenerator

import cats.Functor
import cats.effect.IO
import cats.implicits._
import weaver._

object UUIDGeneratorSuite extends SimpleIOSuite {
  test("discover and use default UUID generator") {
    trait FakeUuidConsumer[F[_]] {
      def doSomething(): F[String]
    }

    object UuidConsumer {
      def impl[F[_]: Functor: UUIDGenerator]: FakeUuidConsumer[F] = () => F.next.map(_.toString)
    }

    UuidConsumer.impl[IO].doSomething().map(uuid => not(expect(uuid.isBlank)))
  }
}
