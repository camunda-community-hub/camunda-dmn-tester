package pme123.camunda.dmn.tester.shared

import pme123.camunda.dmn.tester.shared.EvalStatus.INFO

case class DmnEvalResult(
                          dmn: Dmn,
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
                             decisionId: String,
                             testInputs: Map[String, String],
                             matchedRules: Seq[MatchedRule],
                             maybeError: Option[EvalError]
                           )

case class Dmn(
                id: String,
                hitPolicy: HitPolicy,
                dmnConfig: DmnConfig,
                rules: Seq[DmnRule]
              )

case class DmnRule(
                    index: Int,
                    ruleId: String,
                    inputs: Seq[String],
                    outputs: Seq[String]
                  )

case class EvalResult(
                       status: EvalStatus,
                       decisionId: String,
                       matchedRules: Seq[MatchedRule],
                       failed: Option[EvalError]
                     )

object EvalResult {

  import EvalStatus._

  def apply(
             decisionId: String,
             matchedRules: Seq[MatchedRule],
             failed: Option[EvalError]
           ): EvalResult = {
    val status = (matchedRules, failed) match {
      case (_, Some(_)) => ERROR
      case (Nil, _) => WARN
      case _ =>
        val check = matchedRules.map(_.hasError)
        if (matchedRules.exists(_.hasError))
          ERROR
        else
          INFO
    }
    EvalResult(status, decisionId, matchedRules, failed)
  }
}

case class MatchedRule(
                        ruleId: String,
                        rowIndex: TestedValue,
                        inputs: Seq[(String, String)],
                        outputs: Seq[(String, TestedValue)]
                      ) {
  def hasError: Boolean = rowIndex.isError || outputs.exists(_._2.isError)

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

sealed trait HitPolicy

object HitPolicy {

  case object UNIQUE extends HitPolicy

  case object FIRST extends HitPolicy

  case object ANY extends HitPolicy

  case object COLLECT extends HitPolicy

  def apply(value: String): HitPolicy =
    value.toUpperCase match {
      case "UNIQUE" => UNIQUE
      case "FIRST" => FIRST
      case "ANY" => ANY
      case "COLLECT" => COLLECT
    }
}
