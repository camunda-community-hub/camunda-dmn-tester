import Settings.{client => cli, server => ser, _}

ThisBuild / evictionErrorLevel := Level.Warn

lazy val root = project
  .settings(
    name := s"$projectName-root",
    commands ++= Seq(ReleaseCmd, ReleaseClientCmd)
  )
  .in(file("."))
  .aggregate(shared.jvm, shared.js, client, server)
  .configure(
    projectSettings,
    preventPublication
  )

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
    .settings(name := s"$projectName-shared")
    .configure(
      projectSettings,
      publicationSettings
    )

lazy val client =
  project
    .settings(name := s"$projectName-client")
    .dependsOn(shared.js)
    .enablePlugins(
      ScalaJSPlugin
    )
    .configure(
      projectSettings,
      cli.settings,
      cli.deps,
      preventPublication
    )

lazy val server =
  project
    .dependsOn(shared.jvm)
    .configure(
      projectSettings,
      ser.settings,
      ser.serverDeps,
      ser.deps, // must be moved?
      ser.docker,
      publicationSettings
    )
    .enablePlugins(JavaAppPackaging)
