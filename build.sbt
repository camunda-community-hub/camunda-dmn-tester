val scala213Version = "2.13.3"

val zioVersion = "1.0.3"
lazy val root = project
  .in(file("."))
  .settings(
    name := "camunda-dmn-tester",
    version := "0.1.0",
    // To make the default compiler and REPL use Dotty
    scalaVersion := scala213Version,
    resolvers += Resolver.mavenLocal, // only needed for dmn-engine SNAPSHOT
    libraryDependencies ++= Seq(
      "org.camunda.bpm.extension.dmn.scala" % "dmn-engine" % "1.5.1-SNAPSHOT",
      "com.lihaoyi" %% "ammonite-ops" % "2.2.0",
      "com.lihaoyi" %% "ujson" % "0.9.5",
      "dev.zio" %% "zio" % zioVersion,
      "dev.zio" %% "zio-test" % zioVersion % "test",
      "dev.zio" %% "zio-test-sbt" % zioVersion % "test"
    )
  )
