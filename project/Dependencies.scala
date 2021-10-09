import sbt._

trait Avro {
  private[this] val organization = "org.apache.avro"

  private[this] val version = "1.10.2"

  val avro = organization % "avro" % version
}

trait Caffeine {
  private[this] val organization = "com.github.blemale"

  private[this] val version = "4.1.0"

  val caffeine = organization %% "scaffeine" % version
}

trait Cats {
  private[this] val organization = "org.typelevel"

  private[this] val catsVersion = "2.6.1"
  private[this] val catsEffectVersion = "2.5.4"
  private[this] val kittensVersion = "2.3.2"

  val catsCore = organization %% "cats-core" % catsVersion
  val catsEffect = organization %% "cats-effect" % catsEffectVersion
  val catsFree = organization %% "cats-free" % catsVersion
  val catsKernel = organization %% "cats-kernel" % catsVersion
  val kittens = organization %% "kittens" % kittensVersion
}

trait CatsScalaCheck {
  private[this] val organization = "io.chrisdavenport"

  private[this] val version = "0.3.1"

  val catsScalaCheck = organization %% "cats-scalacheck" % version
}

trait CaseInsensitive {
  private[this] val organization = "org.typelevel"

  private[this] val version = "1.1.4"

  val caseInsensitive = organization %% "case-insensitive" % version
}

trait Circe {
  private[this] val organization = "io.circe"
  private[this] val version = "0.14.1"

  val circeCore = organization %% "circe-core" % version
  // for auto-derivation of JSON codecs
  val circeGeneric = organization %% "circe-generic" % version
  // for string interpolation to JSON model
  val circeLiteral = organization %% "circe-literal" % version
  // codecs for refined types
  val circeRefined = organization %% "circe-refined" % version
}

trait Ciris {
  private[this] val organization = "is.cir"

  private[this] val version = "1.2.1"

  val ciris = organization %% "ciris" % version
  val cirisRefined = organization %% "ciris-refined" % version
}

trait Doobie {
  private[this] val organization = "org.tpolecat"

  private[this] val version = "0.13.4"

  val doobieCore = organization %% "doobie-core" % version
  val doobieFree = organization %% "doobie-free" % version
  val doobieHikari = organization %% "doobie-hikari" % version
  val doobiePostgres = organization %% "doobie-postgres" % version
  val doobieRefined = organization %% "doobie-refined" % version
}

trait Fs2 {
  private[this] val organization = "co.fs2"

  private[this] val version = "2.5.9"

  val fs2Core = organization %% "fs2-core" % version
}

trait Fs2Kafka {
  private[this] val organization = "com.github.fd4s"

  private[this] val version = "1.8.0"

  val fs2Kafka = organization %% "fs2-kafka" % version
  val fs2KafkaVulcan = organization %% "fs2-kafka-vulcan" % version
}

trait Hikari {
  private[this] val organization = "com.zaxxer"

  private[this] val version = "4.0.3"

  val hikariCP = (organization % "HikariCP" % version)
    .exclude("org.slf4j", "slf4j-api")
}

trait Http4s {
  private[this] val organization = "org.http4s"

  private[this] val version = "0.22.5"

  val http4sBlazeServer = organization %% "http4s-blaze-server" % version
  val http4sCirce = organization %% "http4s-circe" % version
  val http4sCore = organization %% "http4s-core" % version
  val http4sDsl = organization %% "http4s-dsl" % version
  val http4sServer = organization %% "http4s-server" % version
}

trait Kafka {
  private[this] val organization = "io.confluent"

  private[this] val version = "6.2.0"

  val kafkaSchemaRegistryClient = organization % "kafka-schema-registry-client" % version
}

trait Logback {
  private[this] val organization = "ch.qos.logback"

  private[this] val version = "1.2.6"

  val logback = organization % "logback-classic" % version
}

trait Log4Cats {
  private[this] val organization = "org.typelevel"

  private[this] val version = "1.3.1"

  val log4CatsCore = organization %% "log4cats-core" % version
  val log4CatsSlf4j = organization %% "log4cats-slf4j" % version
}

trait Magnolia {
  private[this] val organization = "com.propensive"

  private[this] val version = "0.17.0"

  val magnolia = organization %% "magnolia" % version
}

trait NewType {
  private[this] val organization = "io.estatico"

  private[this] val version = "0.4.4"

  val newType = organization %% "newtype" % version
}

trait Refined {
  private[this] val organization = "eu.timepit"

  private[this] val version = "0.9.27"

  val refined = organization %% "refined" % version
  val refinedCats = organization %% "refined-cats" % version
  val refinedScalaCheck = organization %% "refined-scalacheck" % version
}

trait ScalaCheck {
  private[this] val organization = "org.scalacheck"

  private[this] val version = "1.15.4"

  val scalaCheck = organization %% "scalacheck" % version
}

trait ScalaLang {
  private[this] val organization = "org.scala-lang"

  val projectScalaVersion = "2.13.6"
  val scalaReflect = organization % "scala-reflect" % projectScalaVersion
}

trait Shapeless {
  private[this] val organization = "com.chuusai"

  private[this] val version = "2.3.7"

  val shapeless = organization %% "shapeless" % version
}

trait Slf4j {
  private[this] val organization = "org.slf4j"

  private[this] val version = "1.7.32"

  val slf4jApi = organization % "slf4j-api" % version
}

trait Tapir {
  private[this] val modelOrganization = "com.softwaremill.sttp.model"
  private[this] val sharedOrganization = "com.softwaremill.sttp.shared"
  private[this] val tapirOrganization = "com.softwaremill.sttp.tapir"

  private[this] val modelVersion = "1.4.15"
  private[this] val sharedVersion = "1.2.6"
  private[this] val tapirVersion = "0.18.3"

  val sttpModelCore = modelOrganization %% "core" % modelVersion
  val sttpSharedCore = sharedOrganization %% "core" % sharedVersion
  val sttpSharedFs2 = sharedOrganization %% "fs2-ce2" % sharedVersion
  val tapirCore = tapirOrganization %% "tapir-core" % tapirVersion
  val tapirHttp4sServer = tapirOrganization %% "tapir-http4s-server" % tapirVersion
  val tapirJsonCirce = tapirOrganization %% "tapir-json-circe" % tapirVersion
  val tapirNewType = tapirOrganization %% "tapir-newtype" % tapirVersion
  val tapirOpenApiCirceYaml = tapirOrganization %% "tapir-openapi-circe-yaml" % tapirVersion
  val tapirOpenApiDocs = tapirOrganization %% "tapir-openapi-docs" % tapirVersion
  val tapirOpenApiModel = tapirOrganization %% "tapir-openapi-model" % tapirVersion
  val tapirSwaggerUI = tapirOrganization %% "tapir-swagger-ui-http4s" % tapirVersion
}

trait Trace4Cats {
  private[this] val organization = "io.janstenpickle"

  private[this] val version = "0.11.1"

  val trace4CatsBase = organization %% "trace4cats-base" % version
  val trace4CatsCore = organization %% "trace4cats-core" % version
  val trace4CatsHttp4sCommon = organization %% "trace4cats-http4s-common" % version
  val trace4CatsHttp4sServer = organization %% "trace4cats-http4s-server" % version
  val trace4CatsInject = organization %% "trace4cats-inject" % version
  val trace4CatsKernel = organization %% "trace4cats-kernel" % version
  val trace4CatsLogExporter = organization %% "trace4cats-log-exporter" % version
  val trace4CatsModel = organization %% "trace4cats-model" % version
}

trait Vulcan {
  private[this] val organization = "com.github.fd4s"

  private[this] val version = "1.7.1"

  val vulcan = organization %% "vulcan" % version
}

trait Weaver {
  private[this] val organization = "com.disneystreaming"

  private[this] val version = "0.6.6"

  val weaverCats = organization %% "weaver-cats" % version
  val weaverScalaCheck = organization %% "weaver-scalacheck" % version
}

object Dependencies
    extends Avro
    with Caffeine
    with Cats
    with CatsScalaCheck
    with CaseInsensitive
    with Circe
    with Ciris
    with Doobie
    with Fs2
    with Fs2Kafka
    with Hikari
    with Http4s
    with Kafka
    with Logback
    with Log4Cats
    with Magnolia
    with NewType
    with Refined
    with ScalaCheck
    with ScalaLang
    with Shapeless
    with Slf4j
    with Tapir
    with Trace4Cats
    with Vulcan
    with Weaver
