val scala213Version = "2.13.3"

val zioVersion = "1.0.3"

lazy val projectSettings = Def.settings(
  organization := "pme123",
  version := "0.1.0",
  scalaVersion := scala213Version,
)

lazy val root = project.in(file("."))
  .aggregate(core, sbtPlugin)
  .settings(projectSettings)
  .settings(publish / skip := true)

lazy val core = project.in(file("./core"))
  .settings(projectSettings)
  .settings(Core.settings)
  .settings(Core.deps)

lazy val sbtPlugin = project.in(file("./sbtPlugin"))
 // .enablePlugins(SbtPlugin)
  .settings(projectSettings)
  .settings(Sbt.settings)
  .settings(Sbt.deps)
  .dependsOn(core)
