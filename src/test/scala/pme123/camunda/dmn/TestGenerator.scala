package pme123.camunda.dmn

import pme123.camunda.dmn.tester.implicits._
import pme123.camunda.dmn.tester.{DmnTester, TesterData, TesterInput}

import scala.language.implicitConversions

object TestGenerator extends App {
  numbersTester()
  countryRiskTester()

  private def numbersTester(): Unit = {
    val numbers = "numbers"
    val number = "number"
    val data = TesterData(
      List(
        TesterInput(
          number,
          List(1, 2, 3, -12, 234234)
        )
      )
    )
    val tester = DmnTester(numbers, numbers)
    tester
      .parsedDmn(tester.testPath)
      .map(tester.run(data, _))
      .map { evaluated =>
        tester.generateDmnTests(data.inputKeys, evaluated)
        tester.printTestResult("Numbers", data.inputKeys, evaluated)
      }
  }

  private def countryRiskTester(): Unit = {

    val countryRisk = "country-risk"
    val currentCountry = "currentCountry"
    val targetCountry = "targetCountry"

    val data = TesterData(
      List(
        TesterInput(
          currentCountry,
          List(
            "CH",
            "ch",
            "DE",
            "OTHER",
            "asdftre longas Input that should be cutted"
          )
        ),
        TesterInput(
          targetCountry,
          List(
            "CH",
            "ch",
            "DE",
            "OTHER",
            "another awful lon text that is cutted"
          )
        )
      )
    )
    val tester = DmnTester(countryRisk, countryRisk)
    tester
      .parsedDmn(tester.testPath)
      .map(tester.run(data, _))
      .map { evaluated =>
        tester.generateDmnTests(data.inputKeys, evaluated)
        tester.printTestResult("Country Risk", data.inputKeys, evaluated)
      }
  }
}
