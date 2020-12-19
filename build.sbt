import Settings.{client => cli, core => cor, server => ser, shared => sha, _}

lazy val root = project
  .settings(name := s"$projectName-root", commands += ReleaseCmd)
  .in(file("."))
  .aggregate(shared.jvm, client, server)
  .configure(
    projectSettings,
    preventPublication
  )

lazy val core = project
  .configure(
    projectSettings,
    preventPublication,
    cor.settings,
    cor.deps
  )

lazy val shared =
  crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Pure)
    .settings(name := s"$projectName-shared")
    .configure(
      projectSettings,
      sha.deps,
      publicationSettings
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
    .dependsOn(shared.jvm)
    .configure(
      projectSettings,
      ser.settings,
      ser.deps,
      cor.deps, // must be moved?
      ser.docker,
      sha.deps,
      publicationSettings
    )
    .enablePlugins(JavaAppPackaging)
