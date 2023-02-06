import $file.BaseScript

//import $ivy.`io.github.pme123::camunda-dmn-tester-server:$testerVersion`,
import pme123.camunda.dmn.tester.server._

private implicit val workDir: os.Path = os.pwd

def runAndPrint(commands: os.Shellable*) = {
  println(s"Commands: ${commands.mkString(", ")}")
  println(os.proc(commands:_*).call())
}

runAndPrint("sbt",
  "clean"
)

DmnUnitTestGenerator(UnitTestGeneratorConfig(
  "pme123.camunda.dmn.tester.demo",
  List("target", "generated-src")
)).run()

try {
  runAndPrint("sbt",
    "-mem",
    "3000",
    "test"
  )
  println("Tests all successful")
} catch{ case ex: Exception =>
  println("Check the Test Report! There are failed Tests.")
}
println("You find the Report here: target/test-reports/index.html")

