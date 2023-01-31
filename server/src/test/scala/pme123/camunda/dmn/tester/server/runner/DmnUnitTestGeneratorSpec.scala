package pme123.camunda.dmn.tester.server.runner

import pme123.camunda.dmn.tester.server.DmnUnitTestGenerator
import pme123.camunda.dmn.tester.shared.conversions._
import pme123.camunda.dmn.tester.shared._
import pme123.camunda.dmn.tester.{shared => dmnTester}
import pme123.camunda.dmn.tester.shared.HandledTesterException.EvalException
import zio.test.Assertion._
import zio.test._
import zio.test.junit.JUnitRunnableSpec
//noinspection TypeAnnotation
object DmnUnitTestGeneratorSpec extends JUnitRunnableSpec {

  lazy val generator = DmnUnitTestGenerator()

  private val decisionId = "my-decision_12"

  def spec =
    suite("DmnUnitTestGeneratorSpec")(
      test("create Class Name") {
        assertZIO(generator.className("ed.af-bf_rf 6f"))(
          equalTo("EdAfBf_rf6fSuite")
        )
      },
      test("create Method Name") {
        assert(generator.methodName(infoRowResult.testInputs))(
          equalTo("Test Inputs: in1 -> hello Peter! | in2 -> 4.5")
        )
      },
      test("check Test Case successful") {
        assert(generator.successfulTestCase("INFO"))(
          containsString("assert(true)") &&
            containsString("INFO")
        )
      },
      test("create EvalException Test Method") {
        for {
          result <- evalExceptionTestMethod
          _ <- print(s"Result:\n$result")
        } yield assert(result)(
          containsString(decisionId) &&
            containsString("\\n - Failed poorly")
        )
      },
      test("create DmnEvalRowResult Test Method WARN") {
        for {
          result <- resultWarnTestMethod
          _ <- print(s"Result:\n$result")
        } yield assert(result)(
          containsString(decisionId) &&
            containsString("WARN:")
        )
      },
      test("create DmnEvalRowResult Test Method INFO") {
        for {
          result <- resultInfoTestMethod
          _ <- print(s"Result:\n$result")
        } yield assert(result)(
          containsString(decisionId) &&
            containsString("Status: INFO")
        )
      },
      test("create DmnEvalRowResult Test Method INFO with TestCase") {
        for {
          result <- resultInfoTestMethodTestCase
          _ <- print(s"Result:\n$result")
        } yield assert(result)(
          containsString(decisionId) &&
            containsString("Status: INFO")
        )
      },
      test("create Test File") {
        for {
          testMethod <- evalExceptionTestMethod
          testMethod1 <- resultInfoTestMethod
          testMethod2 <- resultWarnTestMethod
          testMethod3 <- resultInfoTestMethodTestCase
          result <- generator.testFile(
            "MyDecision_12Suite",
            testMethod,
            testMethod1,
            testMethod2,
            testMethod3
          )
          _ <- print(s"Result:\n$result")
        } yield assert(result)(
          containsString("package pme123.camunda.dmn.tester.test") &&
            containsString("class MyDecision_12Suite")
        )
      },
      test("generate Tests") {
        assertZIO(generator.generate())(isUnit)
      }
    )

  private val testCase: TestCase = TestCase(
    TesterValue.valueMap(infoRowResultTestCase.testInputs),
    List(dmnTester.TestResult(1, Map("out1" -> "val1", "out2" -> "val2")),dmnTester.TestResult(1, Map("out1" -> "val1", "out2" -> "val3")))
  )
  private lazy val dmnConfig = DmnConfig(
    decisionId,
    TesterData(
      List(
        TesterInput("in1", nullValue = false, List("hello3", "hello")),
        TesterInput("in2", nullValue = true, List(3.5, 4))
      ),
      List.empty,
      List(
        testCase
      )
    ),
    List.empty
  )
  private lazy val dmn =
    Dmn(
      decisionId,
      HitPolicy.UNIQUE,
      dmnConfig,
      Seq.empty
    )
  private lazy val dmnResult =
    DmnEvalResult(
      dmn,
      Seq("in1", "in2"),
      Seq("out1", "out2"),
      Seq.empty,
      Seq.empty
    )

  private lazy val matchedRule = MatchedRule(
    "asdfe4",
    NotTested("1"),
    Seq("in1" -> "hello3", "in2" -> "4.5"),
    Seq("out1" -> NotTested("val1"), "out2" -> NotTested("val2"))
  )
  private lazy val infoRowResult = DmnEvalRowResult(
    EvalStatus.INFO,
    decisionId,
    Map("in1" -> "hello Peter! how are you.", "in2" -> "4.5"),
    Seq(
      matchedRule
    ),
    None
  )

  private lazy val infoRowResultTestCase = DmnEvalRowResult(
    EvalStatus.INFO,
    decisionId,
    Map("in1" -> "hello", "in2" -> "4"),
    Seq(
      matchedRule
    ),
    None
  )

  private lazy val warnRowResult = DmnEvalRowResult(
    EvalStatus.WARN,
    decisionId,
    Map("in1" -> "hello", "in2" -> "3"),
    Seq(
      matchedRule
    ),
    Some(EvalError("This was wrong"))
  )

  private lazy val resultWarnTestMethod =
    generator.testMethod(
      warnRowResult,
      dmnResult
    )

  private lazy val resultInfoTestMethod = {
    generator.testMethod(
      infoRowResult,
      dmnResult
    )
  }

  private lazy val resultInfoTestMethodTestCase = {
    generator.testMethod(
      infoRowResultTestCase,
      dmnResult
    )
  }

  private def evalExceptionTestMethod = {
    generator.testMethod(
      EvalException(dmnConfig, "Failed poorly")
    )
  }
}
