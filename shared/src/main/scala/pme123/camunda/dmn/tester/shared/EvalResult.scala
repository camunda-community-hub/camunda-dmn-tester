package pme123.camunda.dmn.tester.shared

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import pme123.camunda.dmn.tester.shared.EvalStatus.INFO

case class DmnEvalResult(
    dmnTables: AllDmnTables,
    inputKeys: Seq[String],
    outputKeys: Seq[String],
    evalResults: Seq[DmnEvalRowResult],
    missingRules: Seq[DmnRule]
) {
  def maxEvalStatus: EvalStatus = {
    val status = evalResults.map(_.status) ++ missingRules.headOption.map(_ =>
      if (dmnTables.dmnConfig.acceptMissingRules) EvalStatus.INFO
      else EvalStatus.WARN
    )
    status.sorted.headOption.getOrElse(INFO)
  }
}

object DmnEvalResult {
  implicit val decoder: Decoder[DmnEvalResult] = deriveDecoder
  implicit val encoder: Encoder[DmnEvalResult] = deriveEncoder
}

case class DmnEvalRowResult(
    status: EvalStatus,
    testInputs: Map[String, String],
    matchedRulesPerTable: Seq[MatchedRulesPerTable],
    maybeError: Option[EvalError]
) {
  lazy val hasNoMatch: Boolean =
    matchedRulesPerTable.flatMap(_.matchedRules).isEmpty
}
object DmnEvalRowResult {
  implicit val decoder: Decoder[DmnEvalRowResult] = deriveDecoder
  implicit val encoder: Encoder[DmnEvalRowResult] = deriveEncoder
}

case class EvalResult(
    status: EvalStatus,
    matchedRules: Seq[MatchedRulesPerTable],
    failed: Option[EvalError]
)

object EvalResult {

  import EvalStatus._

  def apply(
      matchedRulesPerTable: Seq[MatchedRulesPerTable]
  ): EvalResult = {
    val matchedRules = matchedRulesPerTable.flatMap(_.matchedRules)
    val maybeError = matchedRulesPerTable.find(_.hasError).flatMap(_.maybeError)
    val status =
      if (matchedRulesPerTable.exists(_.hasError))
        ERROR
      else if (matchedRules.isEmpty)
        WARN
      else
        INFO

    EvalResult(status, matchedRulesPerTable, maybeError)
  }

  implicit val decoder: Decoder[EvalResult] = deriveDecoder
  implicit val encoder: Encoder[EvalResult] = deriveEncoder

}

case class MatchedRulesPerTable(
    decisionId: String,
    matchedRules: Seq[MatchedRule],
    maybeError: Option[EvalError]
) {
  def hasError: Boolean = maybeError.nonEmpty || matchedRules.exists(_.hasError)

  def isForMainTable(decId: String): Boolean = decId == decisionId

  lazy val inputKeys: Seq[String] =
    matchedRules.headOption.toSeq
      .flatMap(_.inputs)
      .map(_._1)
  lazy val outputKeys: Seq[String] =
    matchedRules.headOption.toSeq
      .flatMap(_.outputs)
      .map(_._1)
}
object MatchedRulesPerTable {
  implicit val decoder: Decoder[MatchedRulesPerTable] = deriveDecoder
  implicit val encoder: Encoder[MatchedRulesPerTable] = deriveEncoder
}

case class MatchedRule(
    ruleId: String,
    rowIndex: TestedValue,
    inputs: Seq[(String, String)],
    outputs: Seq[(String, TestedValue)]
) {
  def hasError: Boolean =
    rowIndex.isError || outputs.exists(_._2.isError)

}
object MatchedRule {
  implicit val decoder: Decoder[MatchedRule] = deriveDecoder
  implicit val encoder: Encoder[MatchedRule] = deriveEncoder
}

sealed trait TestedValue {

  def value: String

  def isError: Boolean = false

  lazy val intValue: Int = value.toIntOption.getOrElse(-1)
}

case class NotTested(value: String) extends TestedValue

case class TestSuccess(value: String) extends TestedValue

object TestedValue {
  implicit val decoder: Decoder[TestedValue] = deriveDecoder
  implicit val encoder: Encoder[TestedValue] = deriveEncoder
}

case class TestFailure(value: String, msg: String) extends TestedValue {
  override def isError = true
}

object TestFailure {
  def apply(msg: String): TestFailure =
    TestFailure(msg, msg)

  implicit val decoder: Decoder[TestFailure] = deriveDecoder
  implicit val encoder: Encoder[TestFailure] = deriveEncoder

}

case class EvalError(msg: String)

object EvalError {
  implicit val decoder: Decoder[EvalError] = deriveDecoder
  implicit val encoder: Encoder[EvalError] = deriveEncoder
}

sealed trait EvalStatus extends Comparable[EvalStatus] {
  def order: Int

  override def compareTo(o: EvalStatus): Int = order.compareTo(o.order)

}

object EvalStatus {

  case object INFO extends EvalStatus {
    val order = 3
  }

  case object WARN extends EvalStatus {
    val order = 2
  }

  case object ERROR extends EvalStatus {
    val order = 1
  }

  implicit val decoder: Decoder[EvalStatus] = deriveDecoder
  implicit val encoder: Encoder[EvalStatus] = deriveEncoder

}
