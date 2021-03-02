import Dependencies._
import Settings._

sbtSettings

lazy val `train-station` = project
  .root("train-station")
  .aggregate(models, `models-circe`, `train-control-panel`)

lazy val models =
  project
    .library("models")
    .mainDependencies(catsCore, newType, refined, shapeless)
    .testDependencies(scalaCheck)

lazy val `models-circe` = project
  .library("models-circe")
  .dependsOn(models)
  .mainDependencies(circeCore, refined)

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
      log4CatsCore,
      log4CatsSlf4j,
      shapeless,
      slf4jApi
    )
    .testDependencies(catsScalaCheck, kittens, scalactic, weaverCats, weaverScalaCheck)
    .runtimeDependencies(logback)
