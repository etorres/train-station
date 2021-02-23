import Dependencies._
import Settings._

sbtSettings

lazy val root = project
  .root("train-station")
  .aggregate(effect, models, `models-circe`, `train-control-panel`)

lazy val effect = project
  .library("effect")
  .mainDependencies(catsCore, catsEffect, newType, refined, shapeless)

lazy val models =
  project
    .library("models")
    .dependsOn(effect)
    .mainDependencies(catsCore, newType, refined, shapeless)

lazy val `models-circe` = project
  .library("models-circe")
  .dependsOn(models)
  .mainDependencies(circeGeneric, circeLiteral, circeRefined)

lazy val `train-control-panel` =
  project
    .application("train-control-panel")
    .dependsOn(models, `models-circe`)
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
