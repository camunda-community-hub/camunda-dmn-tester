package pme123.camunda.dmn

import org.camunda.dmn.DmnEngine.Result
import org.junit.Test
import pme123.camunda.dmn.tester.DmnTester

import scala.language.implicitConversions

//noinspection TypeAnnotation
class NumbersManualTest {

  val numbers = "numbers"
  val number = "number"
  val result = "result"
  val otherResult = "otherResult"

  val tester = DmnTester(numbers, numbers)
  val testPath = tester.testPath

  @Test
  def testConst(): Unit = {
    tester.runDmnTest(
      testPath,
      Map(number -> 1),
      Result(Map(result -> 1, otherResult -> "first"))
    )
  }

  @Test
  def testInputLessThan(): Unit = {
    tester.runDmnTest(
      testPath,
      Map(number -> 2),
      Result(Map(result -> 2, otherResult -> "second"))
    )
  }

    @Test
  def testConstNull(): Unit = {
    tester.runDmnTest(
      testPath,
      Map(number -> null),
      Result(Map(result -> 99, otherResult -> "NOT HANDLED"))
    )
  }

}
