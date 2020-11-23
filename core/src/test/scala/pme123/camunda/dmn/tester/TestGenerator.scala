package pme123.camunda.dmn.tester

import zio.Runtime

/** generates Unit Tests.
  */
object TestGenerator extends App {
  numbersTester()
  countryRiskTester()
  lazy val runtime = Runtime.default
  private def numbersTester(): Unit = {
    val config =
      runtime.unsafeRun(
        DmnConfigHandler.read(RunnerConfig.defaultBasePath :+ "numbers.conf")
      )
    val tester = DmnTester(config.decisionId, config.dmnPath)
    val data = config.data

    tester
      .parsedDmn()
      .map(tester.run(data, _))
      .map { results =>
        DmnUnitTestGenerator(config.decisionId, config.dmnPath)
          .generate(data.inputKeys, results.results)
      }
  }

  private def countryRiskTester(): Unit = {

    val config =
      runtime.unsafeRun(
        DmnConfigHandler.read(
          RunnerConfig.defaultBasePath :+ "country-risk.conf"
        )
      )
    val tester = DmnTester(config.decisionId, config.dmnPath)
    val data = config.data

    tester
      .parsedDmn()
      .map(tester.run(data, _))
      .map { results =>
        DmnUnitTestGenerator(config.decisionId, config.dmnPath)
          .generate(data.inputKeys, results.results)
      }
  }
}
