package pme123.camunda.dmn

import pme123.camunda.dmn.tester.TesterValue.RandomInts
import pme123.camunda.dmn.tester.conversions.{given _}
import pme123.camunda.dmn.tester._

import scala.language.implicitConversions

object TestGenerator extends App {
  numbersTester()
  countryRiskTester()
  
  private def numbersTester(): Unit = {
    val numbers = "numbers"
    val number = "number"
    val config = DmnConfigHandler.read(RunnerConfig.defaultBasePath :+ "numbers.json")
    val tester = DmnTester(config.decisionId, config.dmnPath)
    val data = config.data

    tester
      .parsedDmn()
      .map(tester.run(data, _))
      .map { evaluated =>
      //  tester.generateDmnTests(data.inputKeys, evaluated)
        tester.printTestResult("Numbers", data.inputKeys, evaluated)
      }
  }

  private def countryRiskTester(): Unit = {

    val countryRisk = "country-risk"
    val currentCountry = "currentCountry"
    val targetCountry = "targetCountry"

    val config = DmnConfigHandler
      .read(RunnerConfig.defaultBasePath :+  "country-risk.json")
    val tester = DmnTester(config.decisionId, config.dmnPath)
    val data = config.data

    tester
      .parsedDmn()
      .map(tester.run(data, _))
      .map { evaluated =>
      //  tester.generateDmnTests(data.inputKeys, evaluated)
        tester.printTestResult("Country Risk", data.inputKeys, evaluated)
      }
  }
}
