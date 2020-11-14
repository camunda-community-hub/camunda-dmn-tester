package pme123.camunda.dmn

import org.camunda.dmn.DmnEngine.Result
import org.junit.Test
import pme123.camunda.dmn.tester.{DmnTester, _}

import scala.language.implicitConversions

//noinspection TypeAnnotation
class CountryRiskManualTest {

  val currentCountry = "currentCountry"
  val targetCountry = "targetCountry"

  val tester = DmnTester("country-risk", Seq("src", "test", "resources", "country-risk.dmn"))

  @Test
  def testCHtoCH(): Unit = {
    tester.runDmnTest(
      Map(currentCountry -> "CH", targetCountry -> "CH"),
      Result(false)
    )
    tester.runDmnTest(
      Map(currentCountry -> "ch", targetCountry -> "ch"),
      Result(false)
    )
  }

  @Test
  def testOther(): Unit = {
    tester.runDmnTest(
      Map(currentCountry -> "AT", targetCountry -> "CH"),
      Result(true)
    )
    tester.runDmnTest(
      Map(currentCountry -> "ch", targetCountry -> "DE"),
      Result(true)
    )
  }
}
