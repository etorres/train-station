package es.eriktorr.train_station
package jdbc.infrastructure

import TrainControlPanelConfig.JdbcConfig

import _root_.doobie._
import _root_.doobie.hikari._
import _root_.doobie.implicits._
import cats.effect._
import cats.implicits._
import ciris.Secret
import eu.timepit.refined.api.Refined
import eu.timepit.refined.cats._

import scala.concurrent.ExecutionContext

object JdbcTestTransactor {
  def testTransactorResource(jdbcConfig: JdbcConfig, currentSchema: String)(
    implicit connectEc: ExecutionContext,
    blocker: Blocker,
    contextShift: ContextShift[IO]
  ): Resource[IO, HikariTransactor[IO]] =
    for {
      transactor <- JdbcTransactor
        .impl[IO](
          jdbcConfig.copy(connectUrl =
            Refined.unsafeApply(s"${jdbcConfig.connectUrl.value}?currentSchema=$currentSchema")
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

  def testJdbcConfig: JdbcConfig = JdbcConfig(
    Refined.unsafeApply("org.postgresql.Driver"),
    Refined.unsafeApply("jdbc:postgresql://localhost:5432/train_station"),
    Refined.unsafeApply("train_station"),
    Secret(Refined.unsafeApply("changeme"))
  )
}
