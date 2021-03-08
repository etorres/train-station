package es.eriktorr.train_station
package jdbc.infrastructure

import shared.infrastructure.TrainControlPanelTestConfig

import _root_.doobie._
import _root_.doobie.hikari._
import _root_.doobie.implicits._
import cats.effect._
import cats.implicits._
import eu.timepit.refined.api.Refined

import scala.concurrent.ExecutionContext

object JdbcTestTransactor {
  def testTransactorResource(currentSchema: String)(
    implicit connectEc: ExecutionContext,
    blocker: Blocker,
    contextShift: ContextShift[IO]
  ): Resource[IO, HikariTransactor[IO]] =
    for {
      transactor <- JdbcTransactor
        .impl[IO](
          TrainControlPanelTestConfig.testConfig.jdbcConfig.copy(connectUrl =
            Refined.unsafeApply(
              s"${TrainControlPanelTestConfig.testConfig.jdbcConfig.connectUrl.value}?currentSchema=$currentSchema"
            )
          )
        )
        .transactorResource
      _ <- truncateAllTablesIn(transactor, currentSchema)
    } yield transactor

  private[this] def truncateAllTablesIn(
    transactor: Transactor[IO],
    currentSchema: String
  ): Resource[IO, Unit] =
    Resource.make {
      (for {
        tableNames <- sql"""
          SELECT table_name
          FROM information_schema.tables
          WHERE table_schema = $currentSchema
          ORDER BY table_name""".query[String].to[List]
        _ <- tableNames
          .map(tableName => Fragment.const(s"truncate table $tableName"))
          .traverse_(_.update.run)
      } yield ()).transact(transactor)
    }(_ => IO.unit)
}
