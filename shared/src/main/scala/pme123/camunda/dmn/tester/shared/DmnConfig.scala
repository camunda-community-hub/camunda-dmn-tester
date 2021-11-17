package pme123.camunda.dmn.tester.shared

import scala.language.implicitConversions
import scala.math.BigDecimal

case class DmnConfig(
    decisionId: String,
    data: TesterData,
    dmnPath: List[String],
    isActive: Boolean = false,
    testUnit: Boolean = true
) {

  def findTestCase(testInputs: Map[String, Any]): Option[TestCase] =
    data.findTestCase(testInputs)

}

case class TesterData(
    inputs: List[TesterInput],
    // simple input-, output-variables used in the DMN
    variables: List[TesterInput] = List.empty,
    testCases: List[TestCase] = List.empty
) {

  lazy val inputKeys: Seq[String] = inputs.map { case TesterInput(k, _, _) =>
    k
  }

  def allInputs(): List[Map[String, Any]] = {
    val data = (inputs ++ variables).map(_.asValues())
    cartesianProduct(data).map(_.toMap)
  }

  /** this creates all variations of the inputs you provide
    */
  def cartesianProduct(
      xss: List[(String, List[Any])]
  ): List[List[(String, Any)]] =
    xss match {
      case Nil => List(Nil)
      case (key, v) :: t =>
        for (xh <- v; xt <- cartesianProduct(t)) yield (key -> xh) :: xt
    }

  def findTestCase(testInputs: Map[String, Any]): Option[TestCase] =
    testCases.find { tc =>
      tc.inputs.view.mapValues(_.value).toMap == testInputs
    }

}

case class TesterInput(
    key: String,
    nullValue: Boolean,
    values: List[TesterValue]
) {

  val valuesAsString: String = values.map(_.valueStr).mkString(", ")

  def valueType: String = values.headOption.map(_.valueType).getOrElse("String")

  def asValues(): (String, List[Any]) = {
    val allValues: List[Any] = values.map(_.value) ++
      (if (nullValue) List(null) else List.empty)
    key -> allValues
  }
}

sealed trait TesterValue {
  def valueStr: String

  def valueType: String

  def value: Any
}

object TesterValue {

  def fromAny(value: Any): TesterValue =
    value match {
      case b: Boolean => BooleanValue(b)
      case n:Long => NumberValue(n)
      case n:Double => NumberValue(n)
      case s: String if s == NullValue.constant => NullValue
      case s: String => StringValue(s)
      case o if o == null => NullValue
      case o => throw new IllegalArgumentException(s"Not expected value type: $o")
    }

  def valueMap(inputs: Map[String, Any]): Map[String, TesterValue] =
    inputs.view.mapValues(fromAny).toMap

  case class StringValue(value: String) extends TesterValue {
    val valueStr: String = value
    val valueType: String = "String"
  }

  case class BooleanValue(value: Boolean) extends TesterValue {
    val valueStr: String = value.toString
    val valueType: String = "Boolean"
  }

  object BooleanValue {
    def apply(strValue: String): BooleanValue =
      BooleanValue(strValue == "true")
  }

  case class NumberValue(value: BigDecimal) extends TesterValue {
    val valueStr: String = value.toString()
    val valueType: String = "Number"
  }

  object NumberValue {
    def apply(strValue: String): NumberValue =
      NumberValue(BigDecimal(strValue))

    def apply(intValue: Int): NumberValue =
      NumberValue(BigDecimal(intValue))

    def apply(longValue: Long): NumberValue =
      NumberValue(BigDecimal(longValue))

    def apply(doubleValue: Double): NumberValue =
      NumberValue(BigDecimal(doubleValue))

  }

  case object NullValue extends TesterValue {
    val valueStr: String = "null"
    val valueType: String = "Null"
    val constant: String = "_NULL_"

    val value: Any = null
  }
}

case class TestCase(
    inputs: Map[String, TesterValue],
    results: List[TestResult]
) {

  lazy val resultsOutputMap: Seq[Map[String, String]] =
    results.map(_.outputs.view.mapValues(_.valueStr).toMap)

  def checkIndex(rowIndex: Int): TestedValue =
    if (results.exists(_.rowIndex == rowIndex))
      TestSuccess(s"$rowIndex")
    else
      TestFailure(s"There is no Output with the Index $rowIndex")

  def checkOut(rowIndex: Int, outputKey: String, value: String): TestedValue =
    results
      .find(_.rowIndex == rowIndex)
      .map(_.checkOut(outputKey, value))
      .getOrElse(TestFailure(s"There is no Output with the Index $rowIndex"))

}

case class TestResult(rowIndex: Int, outputs: Map[String, TesterValue]) {

  def checkOut(outputKey: String, value: String): TestedValue =
    outputs
      .get(outputKey)
      .map(v =>
        if (v.valueStr == value)
          TestSuccess(value)
        else
          TestFailure(
            value,
            s"The output '$outputKey' did not succeed: \n- expected: '${v.valueStr}'\nactual : '$value'"
          )
      )
      .getOrElse(TestFailure(s"There is no Output with Key '$outputKey'"))

}

object conversions {

  implicit def stringToTesterValue(x: String): TesterValue =
    TesterValue.StringValue(x)

  implicit def intToTesterValue(x: Int): TesterValue =
    TesterValue.NumberValue(BigDecimal(x))

  implicit def longToTesterValue(x: Long): TesterValue =
    TesterValue.NumberValue(BigDecimal(x))

  implicit def doubleToTesterValue(x: Double): TesterValue =
    TesterValue.NumberValue(BigDecimal(x))

  implicit def booleanToTesterValue(x: Boolean): TesterValue =
    TesterValue.BooleanValue(x)
}
