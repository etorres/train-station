import Dependencies._
import Settings._

sbtSettings

lazy val root = (project in file("."))
  .settings(Seq(name := "train-station", skip in publish := true))
  .aggregate(effect, models)

lazy val effect = project
  .module("effect")
  .mainDependencies(newType, refined, shapeless)

lazy val models =
  project
    .module("models")
    .dependsOn(effect)
    .mainDependencies(catsCore, newType, refined, shapeless)
    .testDependencies(weaverFramework)
