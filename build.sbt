import Dependencies._
import Settings._

sbtSettings

lazy val root = project
  .root("train-station")
  .aggregate(`circe-models`, effect, models, `train-control-panel`)

lazy val `circe-models` = project
  .library("circe-models")
  .dependsOn(models)
  .mainDependencies(circeGeneric, circeLiteral, circeRefined)

lazy val effect = project
  .library("effect")
  .mainDependencies(catsEffect, newType, refined, shapeless)

lazy val models =
  project
    .library("models")
    .dependsOn(effect)
    .mainDependencies(catsCore, newType, refined, shapeless)

lazy val `train-control-panel` =
  project
    .application("train-control-panel")
    .dependsOn(models)
    .mainDependencies(
      catsEffect,
      circeGeneric,
      circeLiteral,
      circeRefined,
      http4sBlazeServer,
      http4sCirce,
      http4sDsl
    )
    .testDependencies(weaverCats, weaverScalaCheck)

lazy val `train-schedule-display` = ???
