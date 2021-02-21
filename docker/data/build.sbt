scalaVersion := "2.13.4"
lazy val testerVersion = scala.io.Source.fromFile("testerVersion").mkString.trim

resolvers += Resolver.bintrayRepo("pme123", "maven")

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.2.2" % Test,
  "com.vladsch.flexmark" % "flexmark-all" % "0.35.10" % Test,
  "pme123" %% "camunda-dmn-tester-server" % testerVersion % Test
)
testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-h", "target/test-reports")
Test / unmanagedSourceDirectories += baseDirectory.value / "target" / "generated-src"
