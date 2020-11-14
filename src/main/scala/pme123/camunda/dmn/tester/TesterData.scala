package pme123.camunda.dmn.tester

import scala.util.Random
import scala.math.BigDecimal

import scala.language.implicitConversions

case class DmnConfig(decisionId: String, data: TesterData, dmnPath: Seq[String])

case class TesterData(
    inputs: List[TesterInput]
) {

  lazy val inputKeys: Seq[String] = inputs.map { case TesterInput(k, _) => k }

  def normalize(): List[Map[String, Any]] = {
    val data = inputs.map(_.normalize())
    cartesianProduct(data).map(_.toMap)
  }

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

  case class NumberValue(value: BigDecimal) extends TesterValue {
    def normalized: Set[Any] = Set(value)
  }

  case class ValueSet(values: Set[TesterValue]) extends TesterValue {
    def normalized: Set[Any] = values.flatMap(_.normalized)
  }

  case class RandomInts(count: Int) extends TesterValue {
    def normalized: Set[Any] = List.fill(count)(Random.nextInt()).toSet
  }
}

object conversions {

    given Conversion[String, TesterValue]:
        def apply(x:String): TesterValue = TesterValue.StringValue(x)

    given Conversion[Int, TesterValue]:
        def apply(x:Int): TesterValue = TesterValue.NumberValue(BigDecimal(x))

    given Conversion[Boolean, TesterValue]:
        def apply(x:Boolean): TesterValue = TesterValue.BooleanValue(x)
}
