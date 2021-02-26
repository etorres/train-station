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
    .testDependencies(scalaCheck)

lazy val `models-circe` = project
  .library("models-circe")
  .dependsOn(models)
  .mainDependencies(catsCore, circeCore, refined)

lazy val `train-control-panel` =
  project
    .application("train-control-panel")
    .dependsOn(models % "test->test;compile->compile", `models-circe`)
    .mainDependencies(
      catsCore,
      catsEffect,
      circeCore,
      circeGeneric,
      fs2Core,
      http4sBlazeServer,
      http4sCirce,
      http4sCore,
      http4sDsl,
      http4sServer,
      shapeless
    )
    .testDependencies(kittens, weaverCats, weaverScalaCheck)

lazy val `train-schedule-display` = ???
