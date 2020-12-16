package pme123.camunda.dmn.tester.server.zzz

import org.camunda.dmn.Audit
import org.camunda.dmn.Audit.{AuditLogListener, DecisionTableEvaluationResult}
import org.camunda.feel.syntaxtree.{Val, ValError}
import org.camunda.feel.valuemapper.ValueMapper
import pme123.camunda.dmn.tester.shared.HandledTesterException.EvalException
import pme123.camunda.dmn.tester.shared._
import zio._
import zio.console.Console

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
        EvalResult(log.rootEntry.id, ins, rules, maybeError)
    }
    runtime.unsafeRun(
      (for {
        evalResult <- ZIO.fromOption(newValue)
        _ <- auditLogRef.update(ex => ex :+ evalResult)
        _ <- auditLogRef.get
      } yield ()).orElseFail(console.putStr("AuditLog not created"))
    )
  }

  def getDmnEvalResults(
      runResults: RunResults
  ): ZIO[Console, EvalException, DmnEvalResult] = {
    for {
      logEntries <- auditLogRef.get
      RunResults(dmn, results) = runResults
      evalResults <- ZIO
        .fromOption(logEntries.groupBy(_.decisionId).get(dmn.id))
        .orElseFail(
          EvalException(dmn.id, s"There is no DMN width id: ${dmn.id}")
        )
      results <-
        UIO(
          DmnEvalResult(
            dmn,
            results.map(_.inputs.view.mapValues(_.toString).toMap),
            evalResults
          )
        )
    } yield results
  }

  private def unwrap(value: Val): String =
    ValueMapper.defaultValueMapper
      .unpackVal(value) match {
      case Some(seq: Seq[_]) => seq.mkString("[", ", ", "]")
      case Some(value)       => value.toString
      case None              => "NO VALUE"
      case value =>
        s"$value"
    }
}

case class RowPrinter(
    evalResults: Seq[EvalResult],
    inputs: Seq[String],
    outputs: Seq[String],
    ruleIds: Seq[String]
) {

  def printMissingRules(): URIO[Console, Unit] =
    ruleIds.filterNot(matchedRuleIds.contains(_)).toList match {
      case Nil =>
        console.putStrLn(s"INFO:      All Rules matched at least ones.")
      case l =>
        printWarning(
          s"WARN:      The following Rules never matched: [${l.map(rowIndex).mkString(", ")}]"
        )
    }

  private lazy val matchedRuleIds =
    evalResults.flatMap(_.matchedRules.map(_.ruleId)).distinct

  private def rowIndex(ruleId: String) =
    s"${ruleIds.indexWhere(_ == ruleId) + 1}: $ruleId"

}
