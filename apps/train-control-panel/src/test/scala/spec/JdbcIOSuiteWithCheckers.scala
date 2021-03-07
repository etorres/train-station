package es.eriktorr.train_station
package spec

import jdbc.infrastructure.JdbcTestTransactor

import _root_.doobie._
import cats.effect._
import weaver._
import weaver.scalacheck._
import _root_.doobie.util.ExecutionContexts

import scala.concurrent.ExecutionContext

trait JdbcIOSuiteWithCheckers extends SimpleIOSuite with Checkers {
  implicit val evEc: ExecutionContext = ExecutionContexts.synchronous
  implicit val evBlocker: Blocker = Blocker.liftExecutionContext(evEc)

  override def maxParallelism: Int = 1
  override def checkConfig: CheckConfig =
    super.checkConfig.copy(minimumSuccessful = 10, perPropertyParallelism = 1)

  def currentSchema: String

  val testResources: Resource[IO, Transactor[IO]] = JdbcTestTransactor.testTransactorResource(
    JdbcTestTransactor.testJdbcConfig,
    currentSchema
  )
}
