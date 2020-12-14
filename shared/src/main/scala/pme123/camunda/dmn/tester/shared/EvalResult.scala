package pme123.camunda.dmn.tester.shared

import pme123.camunda.dmn.tester.shared.EvalStatus.INFO

case class DmnEvalResult(
    dmn: Dmn,
    inputs: Seq[Map[String, String]],
    evalResults: Seq[EvalResult]
) {
  def maxEvalStatus: EvalStatus =
    evalResults.map(_.status).sorted.headOption.getOrElse(INFO)
}

case class Dmn(id: String, hitPolicy: String, ruleIds: Seq[String])

case class EvalResult(
    status: EvalStatus,
    decisionId: String,
    inputs: Map[String, String],
    matchedRules: Seq[MatchedRule],
    failed: Option[EvalError]
)

object EvalResult {
  import EvalStatus._

  def failed(errorMsg: String): EvalResult =
    apply("test", Map.empty, Seq.empty, Some(EvalError(errorMsg)))

  def successSingle(value: Any): EvalResult =
    apply(
      "test",
      Map.empty,
      Seq(MatchedRule("someRule", Map("single" -> value.toString))),
      None
    )

  def successMap(resultMap: Map[String, Any]): EvalResult =
    apply(
      "test",
      Map.empty,
      Seq(MatchedRule("someRule", resultMap.view.mapValues(_.toString).toMap)),
      None
    )

  lazy val noResult: EvalResult =
    apply("test", Map.empty, Seq.empty, None)

  def apply(
      decisionId: String,
      inputs: Map[String, String],
      matchedRules: Seq[MatchedRule],
      failed: Option[EvalError]
  ): EvalResult = {
    val status = (matchedRules, failed) match {
      case (_, Some(_)) => ERROR
      case (Nil, _)     => WARN
      case _            => INFO
    }
    EvalResult(status, decisionId, inputs, matchedRules, failed)
  }
}

case class MatchedRule(ruleId: String, outputs: Map[String, String])
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
