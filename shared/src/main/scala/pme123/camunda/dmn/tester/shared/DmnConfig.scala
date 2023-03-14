package pme123.camunda.dmn.tester.shared

import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder}
import pme123.camunda.dmn.tester.shared.conversions._

import java.time._
import java.util.Date
import scala.language.implicitConversions
import scala.util.Random

case class DmnConfig(
    decisionId: String = "",
    data: TesterData = TesterData(),
    dmnPath: List[String] = List.empty,
    isActive: Boolean = false,
    testUnit: Boolean = true,
    // if you have lots of inputs, and you don't want to cover all of them
    acceptMissingRules: Boolean = false
) {

  lazy val dmnPathStr: String =
    dmnPath.map(_.trim).filter(_.nonEmpty).mkString("/")
  lazy val dmnConfigPathStr = s"$decisionId${if (testUnit) "" else "-INT"}.conf"
  lazy val inputKeys: Seq[String] = data.inputKeys

  def findTestCase(testInputs: Map[String, Any]): Option[TestCase] =
    data.findTestCase(testInputs)

  lazy val decisionIdError: Option[String] = {
    val regex =
      """^(?!xml|Xml|xMl|xmL|XMl|xML|XmL|XML)[A-Za-z_][A-Za-z0-9-_.]*$""".r
    if (regex.matches(decisionId)) None
    else Some(s"This must be a correct XML identifier (regex: $regex)")
  }
  lazy val dmnPathError: Option[String] = {
    val regex = """^([^\\/?%*:|"<>.])+(/[^\\/?%*:|"<>.]+)*\.dmn$""".r
    if (regex.matches(dmnPathStr)) None
    else
      Some(
        s"This must be a correct Path e.g 'myDmns/countryTable.dmn' (regex: $regex)"
      )
  }

  lazy val hasErrors: Boolean =
    decisionIdError.nonEmpty || dmnPathError.nonEmpty

}

object DmnConfig {
  implicit val decoder: Decoder[DmnConfig] = deriveDecoder
  implicit val encoder: Encoder[DmnConfig] = deriveEncoder
}

case class TesterData(
    inputs: List[TesterInput] = List.empty,
    // simple input-, output-variables used in the DMN
    variables: List[TesterInput] = List.empty,
    testCases: List[TestCase] = List.empty
) {

  lazy val inputKeys: Seq[String] = inputs.map(_.key)

  def allInputs(): List[Map[String, Any]] = {
    val data = (inputs ++ variables).map(_.asValues())
    cartesianProduct(data).map(_.toMap)
  }

  /** this creates all variations of the inputs you provide
    */
  private def cartesianProduct(
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

object TesterData {
  implicit val decoder: Decoder[TesterData] = deriveDecoder
  implicit val encoder: Encoder[TesterData] = deriveEncoder
}

case class TesterInput(
    key: String,
    nullValue: Boolean,
    values: List[TesterValue],
    id: Option[Int]
) {

  val valuesAsString: String = values.map(_.valueStr).mkString(", ")

  lazy val withId: TesterInput = this match {
    case ti: TesterInput if ti.id.isEmpty =>
      ti.copy(id = Some(Random.nextInt(100000)))
    case o => o
  }

  def valueType: String = values
    .map(_.valueType)
    .foldLeft(values.headOption.map(_.valueType).getOrElse("String"))(
      (r, tpe) => if (r == tpe) r else "String" //
    )

  def asValues(): (String, List[Any]) = {
    val allValues: List[Any] = values.map {
      _.value
    } ++
      (if (nullValue) List(null) else List.empty)
    key -> allValues
  }

  lazy val keyError: Option[String] = {
    val regex = """^[A-Za-z_][A-Za-z0-9-_.]*$""".r
    if (regex.matches(key)) None
    else Some(s"This must be a correct key - e.g. 'contractId' (regex: $regex)")
  }

  lazy val valuesError: Option[String] = {
    if (valuesAsString.trim.nonEmpty) None
    else Some("Values are required. Examples: '1,2,3', 'hello', 'true, false'")
  }

  lazy val hasErrors: Boolean = keyError.nonEmpty || valuesError.nonEmpty

}

object TesterInput {
  def apply(
      key: String = "",
      nullValue: Boolean = false,
      values: List[TesterValue] = List.empty
  ): TesterInput =
    TesterInput(key, nullValue, values, id = Some(Random.nextInt(100000)))

  def unapply(
      input: TesterInput
  ): Option[(String, Boolean, List[TesterValue])] = Some(
    (input.key, input.nullValue, input.values)
  )

  implicit val decoder: Decoder[TesterInput] = deriveDecoder
  implicit val encoder: Encoder[TesterInput] = deriveEncoder

}
sealed trait TesterValue {
  def valueStr: String

  def valueType: String

  def value: Any
}

object TesterValue {

  def fromAny(value: Any): TesterValue = {
    value match {
      case b: Boolean       => BooleanValue(b)
      case n: Int           => NumberValue(n)
      case n: Long          => NumberValue(n)
      case n: Float         => NumberValue(n)
      case n: Double        => NumberValue(n)
      case d: LocalDateTime => DateValue(d)
      case d: Date =>
        DateValue(
          Instant
            .ofEpochMilli(d.getTime())
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
        )
      case s: String if s == NullValue.constant   => NullValue
      case s: String if s.trim.matches(dateRegex) => DateValue(s)
      case s: String                              => StringValue(s)
      case o if o == null                         => NullValue
      case o =>
        throw new IllegalArgumentException(s"Not expected value type: ${o.getClass} ($o)")
    }
  }

  def fromString(value: String): TesterValue =
    value match {
      case "null"                                 => NullValue
      case "true"                                 => BooleanValue(true)
      case "false"                                => BooleanValue(false)
      case s if s.trim.matches(longRegex)         => NumberValue(s.toLong)
      case s if s.trim.matches(doubleRegex)       => NumberValue(s.toDouble)
      case s: String if s.trim.matches(dateRegex) => DateValue(s)
      case s: String                              => StringValue(s)
    }

  def valueMap(inputs: Map[String, String]): Map[String, TesterValue] =
    inputs.view.mapValues(fromString).toMap

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

  lazy val datePattern = "yyyy-MM-dd'T'HH:mm:ss"
  lazy val dateFormat = format.DateTimeFormatter.ofPattern(datePattern)

  case class DateValue(value: LocalDateTime) extends TesterValue {
    val valueStr: String = dateFormat.format(value)
    val valueType: String = "Date"
  }
  object DateValue {
    def apply(dateStr: String): DateValue =
      DateValue(LocalDateTime.parse(dateStr, dateFormat))
  }

  case object NullValue extends TesterValue {
    val valueStr: String = "null"
    val valueType: String = "Null"
    val constant: String = "_NULL_"

    val value: Any = null
  }

  implicit val decoder: Decoder[TesterValue] = deriveDecoder
  implicit val encoder: Encoder[TesterValue] = deriveEncoder

}

case class TestCase(
    inputs: Map[String, TesterValue],
    results: List[TestResult]
) {

  def checkIndex(rowIndex: Int): TestedValue =
    if (results.exists(_.rowIndex == rowIndex))
      TestSuccess(s"$rowIndex")
    else
      TestFailure(s"There is no Index $rowIndex")

  def checkOut(rowIndex: Int, outputKey: String, value: String): TestedValue =
    results
      .find(_.rowIndex == rowIndex)
      .map(_.checkOut(outputKey, value))
      .getOrElse(TestFailure(s"There is no Output with the Index $rowIndex"))

}

object TestCase {
  implicit val decoder: Decoder[TestCase] = deriveDecoder
  implicit val encoder: Encoder[TestCase] = deriveEncoder
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

object TestResult {
  implicit val decoder: Decoder[TestResult] = deriveDecoder
  implicit val encoder: Encoder[TestResult] = deriveEncoder
}

object conversions {
  val longRegex = """^(-?)(0|([1-9][0-9]*))$"""
  val doubleRegex = """^(-?)(0|([1-9][0-9]*))(\\.[0-9]+)?$"""
  val dateRegex =
    """^([0-9]{4})-(1[0-2]|0[1-9])-(3[01]|0[1-9]|[12][0-9])T(2[0-3]|[01][0-9]):([0-5][0-9]):?([0-5][0-9])?$"""

  implicit def stringToTesterValue(x: String): TesterValue = {
    if (x.trim.matches(dateRegex))
      TesterValue.DateValue(x)
    else
      TesterValue.StringValue(x)
  }

  implicit def intToTesterValue(x: Int): TesterValue =
    TesterValue.NumberValue(BigDecimal(x))

  implicit def longToTesterValue(x: Long): TesterValue =
    TesterValue.NumberValue(BigDecimal(x))

  implicit def doubleToTesterValue(x: Double): TesterValue =
    TesterValue.NumberValue(BigDecimal(x))

  implicit def booleanToTesterValue(x: Boolean): TesterValue =
    TesterValue.BooleanValue(x)
}
