package pme123.camunda.dmn

import pme123.camunda.dmn.tester.TesterValue.RandomInts
import pme123.camunda.dmn.tester.conversions.given
import pme123.camunda.dmn.tester._

import scala.language.implicitConversions

object TestGenerator extends App {
  numbersTester()
 // countryRiskTester()

  private def numbersTester(): Unit = {
    val numbers = "numbers"
    val number = "number"
    val config = DmnConfigHandler(
      Seq("src", "test", "resources", "numbers.json")
    ).read()
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

    val config = DmnConfigHandler(
      Seq("src", "test", "resources", "country-risk.json")
    ).read()
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
