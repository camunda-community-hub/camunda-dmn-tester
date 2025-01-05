import Settings.{client => cli, server => ser, _}

ThisBuild / evictionErrorLevel := Level.Warn

lazy val root = project
  .settings(
    name := s"$projectName-root",
    commands ++= Seq(ReleaseCmd, ReleaseClientCmd),
    crossScalaVersions := Nil,
    projectSettings,
    preventPublication
  )
  .in(file("."))
  .aggregate(shared.jvm, shared.js, client, server)

lazy val shared =
  crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Pure)
    .settings(
      libraryDependencies ++= Seq(
        "io.circe" %%% "circe-generic" % Deps.version.circe,
        "io.circe" %%% "circe-parser" % Deps.version.circe,
        "io.github.cquiroz" %%% "scala-java-time" % Deps.version.scalaJavaTime
      ),
      buildInfoKeys := Seq[BuildInfoKey](name, version),
      buildInfoPackage := projectPackage + ".camunda.dmn.tester",
      buildInfoOptions := Seq(
        BuildInfoOption.ToJson, // Add a toJson method to BuildInfo
        BuildInfoOption.ToMap, // Add a toMap method to BuildInfo
        BuildInfoOption.BuildTime // Add timestamp values
      )
    )
    .enablePlugins(BuildInfoPlugin)
    .jvmSettings(
      scalaVersion := scala2V,
      crossScalaVersions := Seq(scala2V, scala3V)
    )
    .jsSettings(
      scalaVersion := scala3V
    )
    .settings(
      name := s"$projectName-shared",
      projectSettings,
      publicationSettings
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
      libraryDependencies ++= (ser.deps ++ ser.serverDeps),
      ser.docker,
      Compile / mainClass := Some(
        "pme123.camunda.dmn.tester.server.HttpServer"
      )
    )
    .enablePlugins(DockerPlugin)
    .enablePlugins(JavaAppPackaging)
