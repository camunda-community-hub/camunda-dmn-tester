import sbt._

object Deps {

  val scala212 = "2.12.11"
  val scala213 = "2.13.3"
  val supportedScalaVersions = List(scala212, scala213)

  object version {
    val ammonite = "2.2.0"
    val dmnScala = "1.5.1-SNAPSHOT"
    val ujson = "0.9.5"
    val zio = "1.0.3"
  }

  val dmnScala =
    "org.camunda.bpm.extension.dmn.scala" % "dmn-engine" % version.dmnScala

  val ammonite = "com.lihaoyi" %% "ammonite-ops" % version.ammonite
  val ujson = "com.lihaoyi" %% "ujson" % version.ujson

  val zio = "dev.zio" %% "zio" % version.zio

  val zioTest = "dev.zio" %% "zio-test" % version.zio
  val zioTestSbt = "dev.zio" %% "zio-test-sbt" % version.zio

}
