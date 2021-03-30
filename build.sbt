import Dependencies._
import Settings._

sbtSettings

lazy val `train-station` = project
  .root("train-station")
  .aggregate(models, `models-circe`, `models-doobie`, `models-vulcan`, `train-control-panel`)
  .dependsOn(models, `models-circe`, `models-doobie`, `models-vulcan`, `train-control-panel`)
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

lazy val `models-doobie` = project
  .library("models-doobie")
  .dependsOn(models)
  .mainDependencies(doobieCore, refined)

lazy val `models-vulcan` = project
  .library("models-vulcan")
  .dependsOn(models)
  .mainDependencies(avro, catsCore, catsFree, catsKernel, refined, vulcan)

lazy val `train-control-panel` =
  project
    .application("train-control-panel")
    .dependsOn(
      effect,
      models % "test->test;compile->compile",
      `models-circe`,
      `models-doobie`,
      `models-vulcan`
    )
    .mainDependencies(
      catsCore,
      catsEffect,
      catsFree,
      catsKernel,
      circeCore,
      circeGeneric,
      ciris,
      cirisRefined,
      doobieCore,
      doobieFree,
      doobieHikari,
      doobiePostgres,
      fs2Core,
      fs2Kafka,
      fs2KafkaVulcan,
      http4sBlazeServer,
      http4sCirce,
      http4sCore,
      http4sDsl,
      http4sServer,
      hikariCP,
      kafkaSchemaRegistryClient,
      kittens,
      log4CatsCore,
      log4CatsSlf4j,
      logback,
      refined,
      refinedCats,
      scalaReflect,
      shapeless,
      slf4jApi,
      trace4CatsBase,
      trace4CatsCore,
      trace4CatsHttp4sCommon,
      trace4CatsHttp4sServer,
      trace4CatsInject,
      trace4CatsKernel,
      trace4CatsLogExporter,
      trace4CatsModel,
      vulcan
    )
    .testDependencies(caffeine, catsScalaCheck, weaverCats, weaverScalaCheck)
