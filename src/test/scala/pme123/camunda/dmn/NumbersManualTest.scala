package pme123.camunda.dmn

import org.camunda.dmn.DmnEngine.Result
import org.junit.Test
import pme123.camunda.dmn.tester._

import scala.language.implicitConversions

//noinspection TypeAnnotation
class NumbersManualTest {

  val numbers = "numbers"
  val number = "number"
  val result = "result"
  val otherResult = "otherResult"

  val tester = DmnTester(numbers, Seq("src", "test", "resources", "numbers.dmn"))

  @Test
  def testConst(): Unit = {
    tester.runDmnTest(
      Map(number -> 1),
      Result(Map(result -> 1, otherResult -> "first"))
    )
  }

  @Test
  def testInputLessThan(): Unit = {
    tester.runDmnTest(
      Map(number -> 2),
      Result(Map(result -> 2, otherResult -> "second"))
    )
  }

  @Test
  def testConstNull(): Unit = {
    tester.runDmnTest(
      Map(number -> null),
      Result(Map(result -> 99, otherResult -> "NOT HANDLED"))
    )
  }

}
