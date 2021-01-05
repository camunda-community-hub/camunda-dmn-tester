import sbt.Keys.libraryDependencies
import sbt._

object Deps {

  val scala212 = "2.12.11"
  val scala213 = "2.13.3"
  val supportedScalaVersions = List(scala212, scala213)

  object version {
    val ammonite = "2.2.0"
    val dmnScala = "1.5.0"
    val http4s = "0.21.12"
    val slf4j = "1.7.30"
    val zio = "1.0.3"
    val zioCats = "2.2.0.1"
    val zioConfig = "1.0.0-RC31-1"
  }

  val dmnScala =
    "org.camunda.bpm.extension.dmn.scala" % "dmn-engine" % version.dmnScala

  val ammonite = "com.lihaoyi" %% "ammonite-ops" % version.ammonite

  val zio = "dev.zio" %% "zio" % version.zio
  val zioCats = "dev.zio" %% "zio-interop-cats" % version.zioCats
  val zioConfigMagnolia = "dev.zio" %% "zio-config-magnolia" % version.zioConfig
  val zioConfigHocon = "dev.zio" %% "zio-config-typesafe" % version.zioConfig

  val zioTest = "dev.zio" %% "zio-test" % version.zio
  val zioTestSbt = "dev.zio" %% "zio-test-sbt" % version.zio

  lazy val http4sDsl = "org.http4s" %% "http4s-dsl" % version.http4s
  lazy val http4sServer = "org.http4s" %% "http4s-blaze-server" % version.http4s
 lazy val slf4j = "org.slf4j" % "slf4j-simple" % version.slf4j
}
