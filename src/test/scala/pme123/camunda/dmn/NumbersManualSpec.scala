package pme123.camunda.dmn

import org.camunda.dmn.DmnEngine.{NilResult, Result}
import pme123.camunda.dmn.tester.EvalResult.{noResult, successMap}
import pme123.camunda.dmn.tester._
import zio.test.Assertion._
import zio.test._

import scala.language.implicitConversions

//noinspection TypeAnnotation
object NumbersManualSpec extends DefaultRunnableSpec {

  val numbers = "numbers"
  val number = "number"
  val result = "result"
  val otherResult = "otherResult"

  val tester =
    DmnTester(numbers, Seq("src", "test", "resources", "numbers.dmn"))

  def spec = suite("CountryRiskManualTest")(
    testM("const") {
      assertM{
        tester.runDmnTest(
          Map(number -> 1),
          EvalResult.failed("multiple values aren't allowed for UNIQUE hit policy. found: 'List(Map(result -> ValNumber(1), otherResult -> ValString(first)), Map(result -> ValNumber(2), otherResult -> ValString(second)))'")
        )// check exception
      }(isNonEmptyString)
    },
    testM("input less than") {
      assertM(
        tester.runDmnTest(
          Map(number -> 2),
          successMap(Map(result -> 2, otherResult -> "second"))
        )
      )(isNonEmptyString)
    },
    testM("no result") {
      assertM(
        tester.runDmnTest(
          Map(number -> null),
          noResult
        )
      )(isNonEmptyString)
    }
  )
}
