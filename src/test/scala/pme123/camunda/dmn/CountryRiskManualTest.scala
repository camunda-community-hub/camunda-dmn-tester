package pme123.camunda.dmn

import org.camunda.dmn.DmnEngine.Result
import org.junit.Test
import pme123.camunda.dmn.tester._
import scala.language.implicitConversions

//noinspection TypeAnnotation
class CountryRiskManualTest {

  val currentCountry = "currentCountry"
  val targetCountry = "targetCountry"

  val tester = DmnTester("country-risk", "country-risk")
  val testPath = tester.testPath // path to where the DMN is located

  @Test
  def testCHtoCH(): Unit = {
    tester.runDmnTest(
      testPath,
      Map(currentCountry -> "CH", targetCountry -> "CH"),
      Result(false)
    )
    tester.runDmnTest(
      testPath,
      Map(currentCountry -> "ch", targetCountry -> "ch"),
      Result(false)
    )
  }

  @Test
  def testOther(): Unit = {
    tester.runDmnTest(
      testPath,
      Map(currentCountry -> "AT", targetCountry -> "CH"),
      Result(true)
    )
    tester.runDmnTest(
      testPath,
      Map(currentCountry -> "ch", targetCountry -> "DE"),
      Result(true)
    )
  }
}
