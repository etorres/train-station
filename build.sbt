import Dependencies._
import Settings._

sbtSettings

lazy val `train-station` = project
  .root("train-station")
  .aggregate(models, `models-circe`, `models-vulcan`, `train-control-panel`)
  .dependsOn(models, `models-circe`, `models-vulcan`, `train-control-panel`)
  .enablePlugins(JavaAppPackaging)
  .settings(mainClass in Compile := Some("es.eriktorr.train_station.TrainControlPanelApp"))

lazy val effect = project.library("effect").mainDependencies(catsCore)

lazy val models =
  project
    .library("models")
    .mainDependencies(catsCore, catsKernel, kittens, newType, refined, shapeless)
    .testDependencies(scalaCheck)

lazy val `models-circe` = project
  .library("models-circe")
  .dependsOn(models)
  .mainDependencies(circeCore, refined)

lazy val `models-vulcan` = project
  .library("models-vulcan")
  .dependsOn(models)
  .mainDependencies(avro, catsCore, catsFree, catsKernel, refined, vulcan)

lazy val `train-control-panel` =
  project
    .application("train-control-panel")
    .dependsOn(effect, models % "test->test;compile->compile", `models-circe`, `models-vulcan`)
    .mainDependencies(
      catsCore,
      catsEffect,
      catsKernel,
      circeCore,
      circeGeneric,
      ciris,
      cirisRefined,
      doobieHikari,
      doobiePostgres,
      doobieRefined,
      fs2Core,
      fs2Kafka,
      fs2KafkaVulcan,
      http4sBlazeServer,
      http4sCirce,
      http4sCore,
      http4sDsl,
      http4sServer,
      kafkaSchemaRegistryClient,
      kittens,
      log4CatsCore,
      log4CatsSlf4j,
      logback,
      refined,
      scalaReflect,
      shapeless,
      slf4jApi,
      vulcan
    )
    .testDependencies(catsScalaCheck, weaverCats, weaverScalaCheck)
