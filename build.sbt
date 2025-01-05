import Settings.{client => cli, server => ser, _}

ThisBuild / evictionErrorLevel := Level.Warn

lazy val root = project
  .settings(
    name := s"$projectName-root",
    commands ++= Seq(ReleaseCmd, ReleaseClientCmd),
    projectSettings,
    preventPublication
  )
  .in(file("."))
  .aggregate(shared.jvm, shared.js, client, server)

lazy val shared =
  crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Pure)
    .jvmSettings(
      scalaVersion := scala2V,
      libraryDependencies ++= Seq(
        "io.circe" %% "circe-generic" % "0.14.6",
        "io.circe" %% "circe-parser" % "0.14.6"
      )
    )
    .jsSettings(
      scalaVersion := scala3V,
      libraryDependencies ++= Seq(
        "io.circe" %%% "circe-generic" % "0.14.6",
        "io.circe" %%% "circe-parser" % "0.14.6"
      )
    )
    .settings(
      name := s"$projectName-shared",
      projectSettings,
      publicationSettings
    )
    .configure(
    )

lazy val client =
  project
    .settings(
      name := s"$projectName-client",
      projectSettings,
      cli.settings,
      cli.deps,
      preventPublication
    )
    .dependsOn(shared.js)
    .enablePlugins(
      ScalaJSPlugin
    )

lazy val server =
  project
    .dependsOn(shared.jvm)
    .settings(
      projectSettings,
      ser.settings,
      publicationSettings,
      libraryDependencies ++= ser.deps ++ ser.serverDeps,
      ser.docker
    )
    .enablePlugins(DockerPlugin)
    .enablePlugins(JavaAppPackaging)
