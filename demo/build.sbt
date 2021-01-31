scalaVersion := "2.13.4"
lazy val testerVersion = scala.io.Source.fromFile("testerVersion").mkString.trim

libraryDependencies ++=Seq(
  "junit" % "junit" % "4.13" % Test,
  "pme123" %% "camunda-dmn-tester-server" % testerVersion % Test,
  "com.novocode" % "junit-interface" % "0.11" % Test exclude("junit", "junit-dep")
)

Test / unmanagedSourceDirectories += baseDirectory.value / "target" / "generated-src"
