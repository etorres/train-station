import sbt._

trait Cats {
  private[this] val organization = "org.typelevel"

  private[this] val catsVersion = "2.4.2"
  private[this] val catsEffectVersion = "2.3.3"
  private[this] val kittensVersion = "2.2.1"

  val catsCore = organization %% "cats-core" % catsVersion
  val catsEffect = organization %% "cats-effect" % catsEffectVersion
  val kittens = organization %% "kittens" % kittensVersion
}

trait CatsScalaCheck {
  private[this] val organization = "io.chrisdavenport"

  private[this] val version = "0.3.0"

  val catsScalaCheck = organization %% "cats-scalacheck" % version
}

trait Circe {
  private[this] val organization = "io.circe"
  private[this] val version = "0.13.0"

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
}

trait Fs2 {
  private[this] val organization = "co.fs2"

  private[this] val version = "2.5.2"

  val fs2Core = organization %% "fs2-core" % version
}

trait Fs2Kafka {
  private[this] val organization = "com.github.fd4s"

  private[this] val version = "1.4.0"

  val fs2Kafka = organization %% "fs2-kafka" % version
}

trait Http4s {
  private[this] val organization = "org.http4s"

  private[this] val version = "0.21.19"

  val http4sBlazeClient = organization %% "http4s-blaze-client" % version
  val http4sBlazeServer = organization %% "http4s-blaze-server" % version
  val http4sCirce = organization %% "http4s-circe" % version
  val http4sDsl = organization %% "http4s-dsl" % version
}

trait NewType {
  private[this] val organization = "io.estatico"

  private[this] val version = "0.4.4"

  val newType = organization %% "newtype" % version
}

trait Refined {
  private[this] val organization = "eu.timepit"

  private[this] val version = "0.9.21"

  val refined = organization %% "refined" % version
  val refinedScalaCheck = organization %% "refined-scalacheck" % version
}

trait Shapeless {
  private[this] val organization = "com.chuusai"
  private[this] val version = "2.3.3"

  val shapeless = organization %% "shapeless" % version
}

trait Weaver {
  private[this] val organization = "com.disneystreaming"

  private[this] val version = "0.6.0-M6"

  val weaverCats = organization %% "weaver-cats" % version
  val weaverScalaCheck = organization %% "weaver-scalacheck" % version
}

object Dependencies
    extends Cats
    with CatsScalaCheck
    with Circe
    with Ciris
    with Fs2
    with Fs2Kafka
    with Http4s
    with NewType
    with Refined
    with Shapeless
    with Weaver
