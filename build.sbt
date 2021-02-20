import Dependencies._
import Settings._

sbtSettings

lazy val root = (project in file("."))
  .settings(Seq(name := "train-station", skip in publish := true))
  .aggregate(`effect-lib`, `models-lib`)

lazy val `effect-lib` = project
  .library("effect")
  .mainDependencies(newType, refined, shapeless)

lazy val `models-lib` =
  project
    .library("models")
    .dependsOn(`effect-lib`)
    .mainDependencies(catsCore, newType, refined, shapeless)
    .testDependencies(weaverFramework)
