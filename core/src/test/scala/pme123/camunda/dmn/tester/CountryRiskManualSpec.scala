package pme123.camunda.dmn.tester

import pme123.camunda.dmn.tester.EvalResult.successSingle
import zio.test.Assertion.isNonEmptyString
import zio.test.{DefaultRunnableSpec, assertM, suite, testM}

//noinspection TypeAnnotation
object CountryRiskManualSpec extends DefaultRunnableSpec {
  val currentCountry = "currentCountry"
  val targetCountry = "targetCountry"

  val tester = DmnTester(
    "country-risk",
    TestProps.baseDmnPath :+ "country-risk.dmn"
  )

  def spec = suite("CountryRiskManualTest")(
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
