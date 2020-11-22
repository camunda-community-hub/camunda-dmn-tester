import sbt.Keys._
import sbt._

object Core {

  lazy val settings: Project => Project = _.settings(
    name := "camunda-dmn-tester",
    scalaVersion := Deps.scala213,
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
    resolvers += Resolver.bintrayRepo("pme123", "projects"),
    resolvers += Resolver.mavenLocal // only needed for dmn-engine SNAPSHOT
//    crossScalaVersions := Deps.supportedScalaVersions
  )

  lazy val deps: Project => Project = _.settings(
    libraryDependencies ++= Seq(
      Deps.ammonite,
      Deps.dmnScala,
      Deps.zio,
      Deps.zioConfigHocon,
      Deps.zioConfigMagnolia,
      Deps.zioTest % Test,
      Deps.zioTestSbt % Test
    )
  )

}
