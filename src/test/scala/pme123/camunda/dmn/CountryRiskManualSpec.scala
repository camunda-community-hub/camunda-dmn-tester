package pme123.camunda.dmn

import pme123.camunda.dmn.tester.DmnTester
import pme123.camunda.dmn.tester.EvalResult.successSingle
import zio.test.Assertion._
import zio.test._

import scala.language.implicitConversions

//noinspection TypeAnnotation
object CountryRiskManualSpec extends DefaultRunnableSpec {
  val currentCountry = "currentCountry"
  val targetCountry = "targetCountry"

  val tester = DmnTester(
    "country-risk",
    Seq("src", "test", "resources", "country-risk.dmn")
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
