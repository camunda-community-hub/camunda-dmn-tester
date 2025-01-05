import sbt.Keys._
import sbt._

object Deps {

  val scala212 = "2.12.17"
  val scala213 = "2.13.15"
  val scala3 = "3.6.2"
  val supportedScalaVersions = List(scala212, scala213, scala3)

  object version {
    val circe = "0.14.10"
    val scalaJavaTime = "2.5.0"
    val osLib = "0.8.1"
    val dmnScala = "1.6.2"
    val http4s = "0.21.12"
    val logback = "1.2.3"
    val zio = "2.1.14"
    val zioCats = "2.2.0.1"
    val zioConfig = "4.0.3"
    val scalaTest = "3.2.2"
  }

  val dmnScala =
    "org.camunda.bpm.extension.dmn.scala" % "dmn-engine" % version.dmnScala
  val osLib = "com.lihaoyi" %% "os-lib" % version.osLib

  val zio = "dev.zio" %% "zio" % version.zio
  val zioCats = "dev.zio" %% "zio-interop-cats" % version.zioCats
  val zioConfigMagnolia = "dev.zio" %% "zio-config-magnolia" % version.zioConfig
  val zioConfigHocon = "dev.zio" %% "zio-config-typesafe" % version.zioConfig

  val zioTest = "dev.zio" %% "zio-test" % version.zio % Test
  val zioTestJUnit = "dev.zio" %% "zio-test-junit" % version.zio % Test
  val zioTestSbt = "dev.zio" %% "zio-test-sbt" % version.zio % Test

  lazy val http4sDsl = "org.http4s" %% "http4s-dsl" % version.http4s
  lazy val http4sServer = "org.http4s" %% "http4s-ember-server" % version.http4s
  lazy val http4sCirce = "org.http4s" %% "http4s-circe" % version.http4s
  lazy val logback = "ch.qos.logback"  %  "logback-classic"     % version.logback
  lazy val scalaTest = "org.scalatest" %% "scalatest-funsuite" % version.scalaTest % Test

}
