package pme123.camunda.dmn.tester.server

import pme123.camunda.dmn.tester.server.runner.DmnTester
import pme123.camunda.dmn.tester.shared.{DmnConfig, TesterData}
import pme123.camunda.dmn.tester.shared.EvalResult.successSingle
import zio.test.Assertion.isNonEmptyString
import zio.test.{DefaultRunnableSpec, assertM, suite, testM}

//noinspection TypeAnnotation
object CountryRiskManualSpec extends DefaultRunnableSpec {
  val currentCountry = "currentCountry"
  val targetCountry = "targetCountry"

  val tester = DmnTester(
    DmnConfig(
      "country-risk",
      TesterData(List.empty),
      TestProps.baseDmnPath :+ "country-risk.dmn"
    )
  )

  def spec = suite("CountryRiskManualSpec")(
    testM("CH to CH") {
      assertM(
        tester.runDmnTest(
          Map(currentCountry -> "CH", targetCountry -> "CH"),
          successSingle(false)
        )
      )(isNonEmptyString)
    },
    testM("ch to ch") {
      assertM(
        tester.runDmnTest(
          Map(currentCountry -> "ch", targetCountry -> "ch"),
          successSingle(false)
        )
      )(isNonEmptyString)
    },
    testM("AT to CH") {
      assertM(
        tester.runDmnTest(
          Map(currentCountry -> "AT", targetCountry -> "CH"),
          successSingle(true)
        )
      )(isNonEmptyString)
    },
    testM("ch to DE") {
      assertM(
        tester.runDmnTest(
          Map(currentCountry -> "ch", targetCountry -> "DE"),
          successSingle(true)
        )
      )(isNonEmptyString)
    }
  )
}
