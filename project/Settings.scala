import Dependencies._
import explicitdeps.ExplicitDepsPlugin.autoImport._
import sbt.Keys._
import sbt._
import sbt.nio.Keys._
import sbtide.Keys._
import scoverage.ScoverageKeys._
import wartremover.Wart
import wartremover.WartRemover.autoImport._

object Settings {
  def sbtSettings: Seq[Def.Setting[_]] =
    addCommandAlias(
      "check",
      "; undeclaredCompileDependenciesTest; unusedCompileDependenciesTest; scalafmtSbtCheck; scalafmtCheckAll"
    ) ++ addCommandAlias("testWithCoverage", "; coverage; test; coverageReport")

  def welcomeMessage: Def.Setting[String] = onLoadMessage :=
    s"""Custom tasks:
       |check - run all project checks
       |testWithCoverage - test with coverage
       |""".stripMargin

  private[this] val warts: Seq[Wart] = Warts.allBut(
    Wart.Any,
    Wart.Nothing,
    Wart.Null, // Disabled due to an unsolved issue of the context-applied Scala compiler plugin
    Wart.Equals,
    Wart.DefaultArguments,
    Wart.Overloading,
    Wart.PublicInference, // context-applied Scala compiler plugin
    Wart.ToString,
    Wart.ImplicitParameter,
    Wart.ImplicitConversion // @newtype
  )

  private[this] def commonSettings(projectName: String): Def.SettingsDefinition =
    Seq(
      name := projectName,
      ThisBuild / organization := "es.eriktorr",
      ThisBuild / version := "1.0.0",
      ThisBuild / scalaVersion := Dependencies.projectScalaVersion,
      ThisBuild / idePackagePrefix := Some("es.eriktorr.train_station"),
      Global / excludeLintKeys += idePackagePrefix,
      Global / cancelable := true,
      Global / fork := true,
      Global / onChangedBuildSource := ReloadOnSourceChanges,
      resolvers += "Confluent" at "https://packages.confluent.io/maven/",
      addCompilerPlugin("org.augustjune" %% "context-applied" % "0.1.4"),
      addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.13.3" cross CrossVersion.full),
      addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1" cross CrossVersion.binary),
      ThisBuild / libraryDependencySchemes ++= Seq(
        "org.typelevel" %% "cats-core" % VersionScheme.EarlySemVer,
        "org.typelevel" %% "cats-effect" % VersionScheme.EarlySemVer,
        "co.fs2" %% "f2s-core" % VersionScheme.EarlySemVer,
        "org.scalacheck" %% "scalacheck" % VersionScheme.EarlySemVer,
        "org.http4s" %% "http4s-dsl" % VersionScheme.EarlySemVer,
        "org.http4s" %% "http4s-server" % VersionScheme.EarlySemVer
      ),
      unusedCompileDependenciesFilter -= moduleFilter("ch.qos.logback", "logback-classic"),
      unusedCompileDependenciesFilter -= moduleFilter("org.tpolecat", "doobie-postgres"),
      testFrameworks += new TestFramework("weaver.framework.CatsEffect"),
      Compile / compile / wartremoverErrors ++= warts,
      Test / compile / wartremoverErrors ++= warts,
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
    def root(rootName: String): Project =
      project.in(file(".")).settings(Seq(name := rootName, publish / skip := true, welcomeMessage))

    private[this] def module(path: String): Project =
      project.in(file(path)).settings(commonSettings(project.id))

    def application(path: String): Project =
      module(s"apps/$path").settings(
        Seq(
          coverageMinimumStmtTotal := 90,
          coverageFailOnMinimum := true,
          coverageExcludedPackages := "es.eriktorr.train_station.*App;es.eriktorr.train_station.*Resources"
        )
      )
    def library(path: String): Project = module("libs/" ++ path)

    private[this] def dependencies_(dependencies: Seq[ModuleID]): Project =
      project.settings(libraryDependencies ++= dependencies)

    def mainDependencies(dependencies: ModuleID*): Project = dependencies_(dependencies)
    def testDependencies(dependencies: ModuleID*): Project =
      dependencies_(dependencies.map(_ % Test))
    def providedDependencies(dependencies: ModuleID*): Project =
      dependencies_(dependencies.map(_ % Provided))
    def runtimeDependencies(dependencies: ModuleID*): Project =
      dependencies_(dependencies.map(_ % Runtime))
  }
}
