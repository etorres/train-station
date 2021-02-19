import Dependencies._
import Settings._

commonSettings

lazy val root = (project in file("."))
  .settings(skip in publish := true)
  .aggregate(`models`)

lazy val `models` = project.testDependencies(weaverFramework)
