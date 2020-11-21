import sbt.Keys._
import sbt._

object Sbt {

  lazy val settings = Def.settings(
    name := "sbt-dmn-tester",
    scalaVersion := Deps.scala213
  //  addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.3.4")
  )

  lazy val deps = Def.settings(libraryDependencies ++= Seq(
  ))

}
