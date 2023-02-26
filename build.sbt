import Settings.{client => cli, server => ser, _}

ThisBuild / evictionErrorLevel := Level.Warn

lazy val root = project
  .settings(
    name := s"$projectName-root",
    commands ++= Seq(ReleaseCmd, ReleaseClientCmd),
    crossScalaVersions := Nil
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
    .settings(
      libraryDependencies ++= Seq(
        "io.circe" %%% "circe-generic" % Deps.version.circe,
        "io.circe" %%% "circe-parser" % Deps.version.circe
      ),
      buildInfoKeys := Seq[BuildInfoKey](name, version),
      buildInfoPackage := projectPackage + ".camunda.dmn.tester"
    )
    .enablePlugins(BuildInfoPlugin)
    .jvmSettings(
      scalaVersion := scala2V,
      crossScalaVersions := Seq(scala2V, scala3V)
    )
    .jsSettings(
      scalaVersion := scala3V
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
