val dottyVersion = "0.27.0-RC1"
val scala213Version = "2.13.1"

val zioVersion = "1.0.3"
lazy val root = project
  .in(file("."))
  .settings(
    name := "camunda-dmn-tester",
    version := "0.1.0",
    // To make the default compiler and REPL use Dotty
    scalaVersion := dottyVersion,
    resolvers += Resolver.mavenLocal, // only needed for dmn-engine SNAPSHOT
    libraryDependencies ++= Seq(
      //  "org.camunda.bpm.dmn" % "camunda-engine-dmn" % "7.14.0",
      //
    ),
    libraryDependencies ++= Seq(
      "org.camunda.bpm.extension.dmn.scala" % "dmn-engine" % "1.5.1-SNAPSHOT",
      "com.lihaoyi" %% "ammonite-ops" % "2.2.0",
      "com.lihaoyi" %% "ujson" % "0.9.5",
      "dev.zio" %% "zio" % zioVersion,
      "dev.zio" %% "zio-test" % zioVersion % "test",
      "dev.zio" %% "zio-test-sbt" % zioVersion % "test"
    ).map(_.withDottyCompat(scalaVersion.value)) :+
      ("com.novocode" % "junit-interface" % "0.11"),
    // To cross compile with Dotty and Scala 2
    crossScalaVersions := Seq(dottyVersion, scala213Version)
  )
