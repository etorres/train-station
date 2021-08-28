package es.eriktorr.train_station
package jdbc.infrastructure

import shared.infrastructure.TrainControlPanelTestConfig

import cats.effect._
import cats.implicits._
import doobie._
import doobie.hikari._
import doobie.implicits._
import eu.timepit.refined.api.Refined

import scala.concurrent.ExecutionContext

object JdbcTestTransactor {
  def testTransactorResource[F[_]: Async: ContextShift](
    currentSchema: String,
    connectEc: ExecutionContext): Resource[F, HikariTransactor[F]] =
    for {
      transactor <- JdbcTransactor
        .impl[F](
          TrainControlPanelTestConfig.testConfig.jdbcConfig.copy(connectUrl =
            Refined.unsafeApply(
              s"${TrainControlPanelTestConfig.testConfig.jdbcConfig.connectUrl.value}?currentSchema=$currentSchema"
            )
          ),
          connectEc,
          blocker
        )
        .transactorResource
      _ <- truncateAllTablesIn(transactor, currentSchema)
    } yield transactor

  private[this] def truncateAllTablesIn[F[_]: Async](
    transactor: Transactor[F],
    currentSchema: String
  ): Resource[F, Unit] =
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
    }(_ => F.unit)
}
