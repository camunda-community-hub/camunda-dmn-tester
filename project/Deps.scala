import sbt.Keys._
import sbt._

object Deps {

  val scala212 = "2.12.11"
  val scala213 = "2.13.3"
  val supportedScalaVersions: Seq[String] = List(scala212, scala213)

  object version {
    val circe = "0.14.5"
    val osLib = "0.8.1"
    val dmnScala = "1.7.4"
    val http4s = "0.23.18"
    val logback = "1.4.5"
    val zio = "2.0.6"
    val zioCats = "22.0.0.0"
    val zioConfig = "3.0.7"
    val scalaTest = "3.2.2"
  }

  val dmnScala =
    "org.camunda.bpm.extension.dmn.scala" % "dmn-engine" % version.dmnScala

  val osLib = "com.lihaoyi" %% "os-lib" % version.osLib

  val zio = "dev.zio" %% "zio" % version.zio
  val zioCats = "dev.zio" %% "zio-interop-cats" % version.zioCats
  val zioConfigMagnolia = "dev.zio" %% "zio-config-magnolia" % version.zioConfig
  val zioConfigHocon = "dev.zio" %% "zio-config-typesafe" % version.zioConfig

  val zioTest = "dev.zio" %% "zio-test" % version.zio
  val zioTestJUnit = "dev.zio" %% "zio-test-junit" % version.zio
  val zioTestSbt = "dev.zio" %% "zio-test-sbt" % version.zio

  lazy val http4sDsl = "org.http4s" %% "http4s-dsl" % version.http4s
  lazy val http4sServer = "org.http4s" %% "http4s-ember-server" % version.http4s
  lazy val http4sCirce = "org.http4s" %% "http4s-circe" % version.http4s
  lazy val logback = "ch.qos.logback"  %  "logback-classic"     % version.logback
  lazy val scalaTest = "org.scalatest" %% "scalatest-funsuite" % version.scalaTest

}
