package pme123.camunda.dmn.tester.shared

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
      EvalStatus.WARN
    )
    status.sorted.headOption.getOrElse(INFO)
  }
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

case class MatchedRule(
    ruleId: String,
    rowIndex: TestedValue,
    inputs: Seq[(String, String)],
    outputs: Seq[(String, TestedValue)]
) {
  def hasError: Boolean =
    rowIndex.isError || outputs.exists(_._2.isError)

}

sealed trait TestedValue {

  def value: String

  def isError: Boolean = false

  lazy val intValue: Int = value.toIntOption.getOrElse(-1)
}

case class NotTested(value: String) extends TestedValue

case class TestSuccess(value: String) extends TestedValue

case class TestFailure(value: String, msg: String) extends TestedValue {
  override def isError = true
}

object TestFailure {
  def apply(msg: String): TestFailure =
    TestFailure(msg, msg)
}

case class EvalError(msg: String)

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

}
