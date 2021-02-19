import Dependencies._
import sbt.Keys._
import sbt._
import sbtide.Keys._
import wartremover.Wart
import wartremover.WartRemover.autoImport._

object Settings {
  lazy val commonSettings: Def.SettingsDefinition = Seq(
    name := "train-station",
    ThisBuild / organization := "es.eriktorr",
    ThisBuild / version := "1.0.0",
    ThisBuild / scalaVersion := "2.13.4",
    ThisBuild / idePackagePrefix := Some("es.eriktorr"),
    Global / excludeLintKeys += idePackagePrefix,
    Global / cancelable := true,
    Global / fork := true,
    addCompilerPlugin("org.augustjune" %% "context-applied" % "0.1.4"),
    addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.11.3" cross CrossVersion.full),
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1" cross CrossVersion.binary),
    dependencyOverrides ++= Seq(catsCore, catsEffect, fs2Core),
    testFrameworks += new TestFramework("weaver.framework.TestFramework"),
    wartremoverErrors in (Compile, compile) ++= warts,
    wartremoverErrors in (Test, compile) ++= warts,
    scalacOptions ++= Seq(
      "-encoding",
      "utf8",
      "-Xfatal-warnings",
      "-Xlint",
      "-Xlint:-byname-implicit",
      "-Ymacro-annotations",
      "-deprecation",
      "-unchecked",
      "-language:higherKinds",
      "-language:implicitConversions",
      "-feature",
      "-explaintypes",
      "-Xcheckinit"
    ),
    javacOptions ++= Seq(
      "-g:none",
      "-source",
      "11",
      "-target",
      "11",
      "-encoding",
      "UTF-8"
    )
  ) ++
    addCommandAlias(
      "check",
      "; undeclaredCompileDependenciesTest; unusedCompileDependenciesTest; scalafmtSbtCheck; scalafmtCheckAll"
    ) ++ welcomeMessage

  private[this] lazy val welcomeMessage: Def.SettingsDefinition = onLoadMessage := {
    s"""Custom tasks:
       |check - run all project checks
       |""".stripMargin
  }

  private[this] lazy val warts: Seq[Wart] = Warts.allBut(
    Wart.Any,
    Wart.Nothing,
    Wart.Equals,
    Wart.DefaultArguments,
    Wart.Overloading,
    Wart.ToString,
    Wart.ImplicitParameter,
    Wart.ImplicitConversion // @newtype
  )

  implicit class ProjectSyntax(project: Project) {
    private[this] def dependencies_(dependencies: Seq[ModuleID]): Project =
      project.settings(libraryDependencies ++= dependencies)
    def mainDependencies(dependencies: ModuleID*): Project = dependencies_(dependencies)
    def testDependencies(dependencies: ModuleID*): Project =
      dependencies_(dependencies.map(_ % Test))
    def providedDependencies(dependencies: ModuleID*): Project =
      dependencies_(dependencies.map(_ % Provided))
  }
}
