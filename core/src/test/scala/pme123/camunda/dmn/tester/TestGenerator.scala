package pme123.camunda.dmn.tester

import zio.Runtime

/** generates Unit Tests.
  */
object TestGenerator extends App {
  numbersTester()
  countryRiskTester()
  val runtime = Runtime.default
  private def numbersTester(): Unit = {
    val config =
      runtime.unsafeRun(
        DmnConfigHandler.read(RunnerConfig.defaultBasePath :+ "numbers.json")
      )
    val tester = DmnTester(config.decisionId, config.dmnPath)
    val data = config.data

    tester
      .parsedDmn()
      .map(tester.run(data, _))
      .map { evaluated =>
        DmnUnitTestGenerator(config.decisionId, config.dmnPath)
          .generate(data.inputKeys, evaluated)
      }
  }

  private def countryRiskTester(): Unit = {

    val config =
      runtime.unsafeRun(
        DmnConfigHandler.read(
          RunnerConfig.defaultBasePath :+ "country-risk.json"
        )
      )
    val tester = DmnTester(config.decisionId, config.dmnPath)
    val data = config.data

    tester
      .parsedDmn()
      .map(tester.run(data, _))
      .map { evaluated =>
        DmnUnitTestGenerator(config.decisionId, config.dmnPath)
          .generate(data.inputKeys, evaluated)
      }
  }
}
