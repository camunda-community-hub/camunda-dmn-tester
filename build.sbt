import java.io.File

val scala213Version = "2.13.3"

val zioVersion = "1.0.3"

lazy val projectSettings: Project => Project = _.settings(
  organization := "pme123",
  version := "0.1.0",
  scalaVersion := scala213Version
)

lazy val root = project
  .in(file("."))
  .configure(projectSettings)
  .settings(publish / skip := true)
  .dependsOn(core)

lazy val core = project
  .in(file("./core"))
  .configure(
    projectSettings,
    publicationSettings,
    Core.settings,
    Core.deps
  )

lazy val publicationSettings: Project => Project = _.settings(
  publishMavenStyle := true,
  homepage := Some(new URL("https://github.com/pme123/camunda-dmn-tester")),
  startYear := Some(2020),
  pomExtra := (
    <scm>
      <connection>scm:git:github.com:/pme123/camunda-dmn-tester</connection>
      <developerConnection>scm:git:git@github.com:pme123/camunda-dmn-tester.git</developerConnection>
      <url>github.com:pme123/camunda-dmn-tester.git</url>
    </scm>
      <developers>
        <developer>
          <id>pme123</id>
          <name>Pascal Mengelt</name>
        </developer>
      </developers>
  ),
  bintrayRepository := {
    if (isSnapshot.value) "projects-snapshots" else "projects"
  }
)

lazy val preventPublication: Project => Project =
  _.settings(
    publish := {},
    publishTo := Some(
      Resolver.file("Unused transient repository", target.value / "fakepublish")
    ),
    publishArtifact := false,
    publishLocal := {},
    packagedArtifacts := Map.empty
  ) // doesn't work - https://github.com/sbt/sbt-pgp/issues/42
