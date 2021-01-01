package pme123.camunda.dmn.tester.shared

import scala.language.implicitConversions
import scala.math.BigDecimal

case class DmnConfig(
    decisionId: String,
    data: TesterData,
    dmnPath: List[String],
    isActive: Boolean = false
) {

  def findTestCase(testInputs: Map[String, String]): Option[TestCase] = data.findTestCase(testInputs)

}

case class TesterData(
    inputs: List[TesterInput],
    testCases: List[TestCase] = List.empty
) {

  lazy val inputKeys: Seq[String] = inputs.map { case TesterInput(k, _) => k }

  def normalize(): List[Map[String, Any]] = {
    val data = inputs.map(_.normalize())
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

  def findTestCase(testInputs: Map[String, String]): Option[TestCase] =
    testCases.find{tc=>
      tc.inputs.view.mapValues(_.valueStr).toMap == testInputs
    }

}

case class TesterInput(key: String, values: List[TesterValue]) {

  val valuesAsString: String = values.map(_.valueStr).mkString(", ")

  def valueType: String = values.headOption.map(_.valueType).getOrElse("String")

  def normalize(): (String, List[Any]) = {
    val allValues: List[Any] = values.flatMap(_.normalized)
    key -> allValues
  }
}

sealed trait TesterValue {
  def valueStr: String
  def valueType: String
  def value: Any
  def normalized: Set[Any]
}

object TesterValue {

  def fromString(valueStr: String): TesterValue =
    valueStr.toBooleanOption
      .map(BooleanValue.apply) orElse
      valueStr.toDoubleOption
        .map(NumberValue.apply) orElse
      valueStr.toLongOption
        .map(NumberValue.apply) getOrElse
      StringValue(valueStr)

  def valueMap(inputs: Map[String, String]): Map[String, TesterValue] =
    inputs.view.mapValues(fromString).toMap

  case class StringValue(value: String) extends TesterValue {
    val valueStr: String = value
    val valueType: String = "String"
    val normalized: Set[Any] = Set(value)
  }

  case class BooleanValue(value: Boolean) extends TesterValue {
    val valueStr: String = value.toString
    val valueType: String = "Boolean"
    def normalized: Set[Any] = Set(value)
  }
  object BooleanValue {
    def apply(strValue: String): BooleanValue =
      BooleanValue(strValue == "true")
  }
  case class NumberValue(value: BigDecimal) extends TesterValue {
    val valueStr: String = value.toString()
    val valueType: String = "Number"
    def normalized: Set[Any] = Set(value)
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

  case class ValueSet(values: Set[TesterValue]) extends TesterValue {
    val value: Set[TesterValue] = values
    val valueStr: String = values.map(_.valueStr).mkString(",")
    val valueType: String = "Set"
    def normalized: Set[Any] = values.flatMap(_.normalized)
  }
}

case class TestCase(inputs: Map[String, TesterValue], rowIndex: Int, outputs: Map[String, TesterValue])

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
