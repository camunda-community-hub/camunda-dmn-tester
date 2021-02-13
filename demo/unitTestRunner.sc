import $file.BaseScript

//import $ivy.`pme123::camunda-dmn-tester-server:$testerVersion`
import pme123.camunda.dmn.tester.server.runner._
import ammonite.ops._

// add here your comma separated list with Paths you have your DMN Tester Configs
val configPaths = "/dmnConfigs"

sys.props.addOne("TESTER_CONFIG_PATHS",  configPaths)

private implicit val workDir: Path = pwd

%.sbt(
  "clean"
)

DmnUnitTestGenerator(UnitTestGeneratorConfig(
  "pme123.camunda.dmn.tester.demo",
  List("target", "generated-src")
)).run()

try {
  %.sbt(
    "-mem",
    "3000",
    "test"
  )
} catch{ case ex: Exception =>
  println("Check the Test Report! There are failed Tests.")
  println("You find the Report here: target/test-reports/index.html")
}

