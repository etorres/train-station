package es.eriktorr.train_station
package spec

import jdbc.infrastructure.JdbcTestTransactor

import cats.effect._
import doobie._
import doobie.util.ExecutionContexts
import weaver._
import weaver.scalacheck._

trait JdbcIOSuiteWithCheckers extends SimpleIOSuite with Checkers {
  override def maxParallelism: Int = 1
  override def checkConfig: CheckConfig =
    super.checkConfig.copy(minimumSuccessful = 20, perPropertyParallelism = 1)

  def currentSchema: String

  private[this] val connectEc = ExecutionContexts.synchronous

  val testResources: Resource[IO, Transactor[IO]] =
    JdbcTestTransactor.testTransactorResource[IO](
      currentSchema,
      connectEc
    )
}
