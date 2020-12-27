package pme123.camunda.dmn.tester.shared

import scala.language.implicitConversions
import scala.math.BigDecimal

case class DmnConfig(
    decisionId: String,
    data: TesterData,
    dmnPath: List[String],
    isActive: Boolean = false
)

case class TesterData(
    inputs: List[TesterInput]
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
}

case class TesterInput(key: String, values: List[TesterValue]) {
  def normalize(): (String, List[Any]) = {
    val allValues: List[Any] = values.flatMap(_.normalized)
    key -> allValues
  }
}

sealed trait TesterValue {
  def normalized: Set[Any]
}

object TesterValue {

  case class StringValue(value: String) extends TesterValue {
    def normalized: Set[Any] = Set(value)
  }

  case class BooleanValue(value: Boolean) extends TesterValue {
    def normalized: Set[Any] = Set(value)
  }
  object BooleanValue {
    def apply(strValue: String): BooleanValue =
      BooleanValue(strValue == "true")
  }
  case class NumberValue(value: BigDecimal) extends TesterValue {
    def normalized: Set[Any] = Set(value)
  }
  object NumberValue {
    def apply(strValue: String): NumberValue =
      NumberValue(BigDecimal(strValue))

    def apply(intValue: Int): NumberValue =
      NumberValue(BigDecimal(intValue))

    def apply(doubleValue: Double): NumberValue =
      NumberValue(BigDecimal(doubleValue))

  }

  case class ValueSet(values: Set[TesterValue]) extends TesterValue {
    def normalized: Set[Any] = values.flatMap(_.normalized)
  }
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
