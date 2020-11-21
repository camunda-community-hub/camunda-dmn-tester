import sbt.Keys._
import sbt._

object Core {

  lazy val settings = Def.settings(
    name := "camunda-dmn-tester",
    scalaVersion := Deps.scala213,
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
    resolvers += Resolver.mavenLocal, // only needed for dmn-engine SNAPSHOT
//    crossScalaVersions := Deps.supportedScalaVersions
  )

  lazy val deps = Def.settings(libraryDependencies ++= Seq(

    Deps.ammonite,
    Deps.dmnScala,
    Deps.ujson,
    Deps.zio,

    Deps.zioTest % Test,
    Deps.zioTestSbt % Test
  ))

}
