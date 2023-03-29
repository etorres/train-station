import Dependencies._
import Settings._

sbtSettings

lazy val `train-station` = project
  .root("train-station")
  .aggregate(models, `models-circe`, `models-doobie`, `models-vulcan`, `train-control-panel`)
  .dependsOn(models, `models-circe`, `models-doobie`, `models-vulcan`, `train-control-panel`)
  .enablePlugins(JavaAppPackaging)
  .settings(Compile / mainClass := Some("es.eriktorr.train_station.TrainControlPanelApp"))

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
  .testDependencies(circeParser, weaverCats)

lazy val `models-doobie` = project
  .library("models-doobie")
  .dependsOn(models)
  .mainDependencies(doobieCore, doobiePostgres, refined)

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
      catsEffectKernel,
      catsFree,
      catsKernel,
      caseInsensitive,
      circeCore,
      circeGeneric,
      ciris,
      cirisRefined,
      doobieCore,
      doobieFree,
      doobieHikari,
      doobiePostgres,
      fs2Core,
      fs2Io,
      fs2Kafka,
      fs2KafkaVulcan,
      http4sEmberServer,
      http4sCore,
      http4sServer,
      hikariCP,
      ip4sCore,
      kafkaSchemaRegistryClient,
      kittens,
      log4CatsCore,
      log4CatsSlf4j,
      logback,
      magnolia,
      newType,
      openApiCirceYaml,
      openApiModel,
      refined,
      refinedCats,
      shapeless,
      sttpModelCore,
      sttpSharedFs2,
      tapirCore,
      tapirJsonCirce,
      tapirHttp4sServer,
      tapirNewType,
      tapirOpenApiDocs,
      tapirSwaggerUI,
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
    .testDependencies(caffeine, catsScalaCheck, http4sCirce, weaverCats, weaverScalaCheck)
