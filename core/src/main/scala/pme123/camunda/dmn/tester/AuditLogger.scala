package pme123.camunda.dmn.tester

import org.camunda.dmn.Audit
import org.camunda.dmn.Audit.{AuditLogListener, DecisionTableEvaluationResult}
import org.camunda.feel.syntaxtree.{Val, ValError}
import org.camunda.feel.valuemapper.ValueMapper
import zio._

case class AuditLogger(auditLogRef: Ref[Seq[EvalResult]])
    extends AuditLogListener {

  val runtime: Runtime[zio.ZEnv] = Runtime.default

  def onEval(log: Audit.AuditLog): Unit = {
    lazy val newValue = Seq(log.rootEntry.result).collectFirst {
      case DecisionTableEvaluationResult(inputs, matchedRules, result) =>
        val maybeError = Seq(result).collectFirst { case ValError(msg) =>
          EvalError(msg)
        }
        val ins = inputs
          .map(i => i.input.name -> unwrap(i.value))
          .toMap
        val rules = matchedRules
          .map(rule =>
            MatchedRule(
              rule.rule.id,
              rule.outputs
                .map(out => out.output.name -> unwrap(out.value))
                .toMap
            )
          )
        EvalResult(ins, rules, maybeError)
    }
    runtime.unsafeRun(
      (for {
        evalResult <- ZIO.fromOption(newValue)
        _ <- auditLogRef.update(ex => ex :+ evalResult)
        _ <- auditLogRef.get
      } yield ()).orElseFail(console.putStr("AuditLog not created"))
    )
  }

  private def unwrap(value: Val): String =
    ValueMapper.defaultValueMapper
      .unpackVal(value) match {
      case Some(seq: Seq[_]) => seq.mkString("[", ", ", "]")
      case Some(value)       => value.toString
      case None              => "NO VALUE"
      case value =>
        value.toString
    }
}

case class EvalResult(
    inputs: Map[String, String],
    matchedRules: Seq[MatchedRule],
    failed: Option[EvalError] = None
)

object EvalResult {
  def failed(errorMsg: String): EvalResult =
    EvalResult(Map.empty, Seq.empty, Some(EvalError(errorMsg)))

  def successSingle(value: Any): EvalResult =
    EvalResult(Map.empty, Seq(MatchedRule("someRule", Map("single" -> value.toString))))

  def successMap(resultMap: Map[String, Any]): EvalResult =
    EvalResult(Map.empty, Seq(MatchedRule("someRule", resultMap.view.mapValues(_.toString).toMap)))

  lazy val noResult: EvalResult =
    EvalResult(Map.empty, Seq.empty)
}
case class MatchedRule(ruleId: String, outputs: Map[String, String])
case class EvalError(msg: String)
