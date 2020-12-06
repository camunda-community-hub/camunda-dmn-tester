import Settings.{client => cli, core => cor, server => ser, shared => sha, _}

lazy val root = project
  .settings(
    name := s"$projectName-root",
    commands += ReleaseCmd)
  .in(file("."))
  .aggregate(core, shared.js, shared.jvm, client, server)
  .configure(
    projectSettings,
    preventPublication
  )

lazy val core = project
  .settings(name := projectName)
  .configure(
    projectSettings,
    publicationSettings,
    cor.settings,
    cor.deps,
    preventPublication
  )

lazy val shared =
  crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Pure)
    .settings(name := s"$projectName-shared")
    .configure(
      projectSettings,
      sha.deps,
      preventPublication
    )

lazy val client =
  project
    .settings(name := s"$projectName-client")
    .dependsOn(shared.js)
    .enablePlugins(
      ScalablyTypedConverterPlugin,
      ScalaJSBundlerPlugin,
      ScalaJSPlugin
    )
    .configure(
      projectSettings,
      sha.deps,
      cli.slinkyBasics,
      cli.webpackSettings,
      cli.antdSettings,
      preventPublication
    )

lazy val server =
  project
    .settings(name := s"$projectName-server")
    .dependsOn(shared.jvm)
    .configure(
      projectSettings,
      ser.deps,
      ser.http4s,
      ser.docker,
      sha.deps,
      preventPublication
    ).enablePlugins(JavaAppPackaging)
