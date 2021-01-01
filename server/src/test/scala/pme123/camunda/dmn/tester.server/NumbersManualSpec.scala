package pme123.camunda.dmn.tester.server

import pme123.camunda.dmn.tester.server.runner.DmnTester
import pme123.camunda.dmn.tester.shared.{DmnConfig, EvalResult, TesterData}
import pme123.camunda.dmn.tester.shared.EvalResult.{noResult, successMap}
import zio.test.Assertion.isNonEmptyString
import zio.test.{DefaultRunnableSpec, assertM, suite, testM}

//noinspection TypeAnnotation
object NumbersManualSpec extends DefaultRunnableSpec {

  val numbers = "numbers"
  val number = "number"
  val result = "result"
  val otherResult = "otherResult"

  val tester =
    DmnTester(
      DmnConfig(
        numbers,
        TesterData(List.empty),
        TestProps.baseDmnPath :+ "numbers.dmn"
      )
    )

  def spec = suite("CountryRiskManualSpec")(
    testM("const") {
      assertM {
        tester.runDmnTest(
          Map(number -> 1),
          EvalResult.failed(
            "multiple values aren't allowed for UNIQUE hit policy. found: 'List(Map(result -> ValNumber(1), otherResult -> ValString(first)), Map(result -> ValNumber(2), otherResult -> ValString(second)))'"
          )
        ) // check exception
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
