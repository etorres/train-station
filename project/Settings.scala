import Dependencies._
import sbt.Keys._
import sbt._
import sbt.nio.Keys._
import sbtide.Keys._
import wartremover.Wart
import wartremover.WartRemover.autoImport._

object Settings {
  def sbtSettings: Seq[Def.Setting[_]] = addCommandAlias(
    "check",
    "; undeclaredCompileDependenciesTest; unusedCompileDependenciesTest; scalafmtSbtCheck; scalafmtCheckAll"
  )

  def welcomeMessage: Def.Setting[String] = onLoadMessage := {
    s"""Custom tasks:
       |check - run all project checks
       |""".stripMargin
  }

  private[this] val warts: Seq[Wart] = Warts.allBut(
    Wart.Any,
    Wart.Nothing,
    Wart.Equals,
    Wart.DefaultArguments,
    Wart.Overloading,
    Wart.ToString,
    Wart.ImplicitParameter,
    Wart.ImplicitConversion // @newtype
  )

  private[this] def commonSettings(projectName: String): Def.SettingsDefinition =
    Seq(
      name := projectName,
      ThisBuild / organization := "es.eriktorr",
      ThisBuild / version := "1.0.0",
      ThisBuild / scalaVersion := "2.13.4",
      ThisBuild / idePackagePrefix := Some("es.eriktorr"),
      Global / excludeLintKeys += idePackagePrefix,
      Global / cancelable := true,
      Global / fork := true,
      Global / onChangedBuildSource := ReloadOnSourceChanges,
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
    )

  implicit class ProjectSyntax(project: Project) {
    def module(path: String): Project = project.in(file(path)).settings(commonSettings(project.id))

    private[this] def dependencies_(dependencies: Seq[ModuleID]): Project =
      project.settings(libraryDependencies ++= dependencies)

    def mainDependencies(dependencies: ModuleID*): Project = dependencies_(dependencies)
    def testDependencies(dependencies: ModuleID*): Project =
      dependencies_(dependencies.map(_ % Test))
    def providedDependencies(dependencies: ModuleID*): Project =
      dependencies_(dependencies.map(_ % Provided))
  }
}
