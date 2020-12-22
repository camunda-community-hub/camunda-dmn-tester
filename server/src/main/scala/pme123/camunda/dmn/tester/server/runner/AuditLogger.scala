package pme123.camunda.dmn.tester.server.runner

import org.camunda.dmn.Audit
import org.camunda.dmn.Audit.{AuditLogListener, DecisionTableEvaluationResult}
import org.camunda.feel.syntaxtree.{Val, ValError}
import org.camunda.feel.valuemapper.ValueMapper
import pme123.camunda.dmn.tester.shared.EvalStatus.{INFO, WARN}
import pme123.camunda.dmn.tester.shared.HandledTesterException.EvalException
import pme123.camunda.dmn.tester.shared._
import zio._
import zio.console.Console

case class AuditLogger(auditLogRef: Ref[Seq[EvalResult]])
    extends AuditLogListener {

  val runtime: Runtime[zio.ZEnv] = Runtime.default

  def onEval(log: Audit.AuditLog): Unit = {
    lazy val newValue = Seq(log.rootEntry.result).collectFirst {
      case DecisionTableEvaluationResult(_, matchedRules, result) =>
        val maybeError = Seq(result).collectFirst { case ValError(msg) =>
          EvalError(msg)
        }
        val rules = matchedRules
          .map(rule =>
            MatchedRule(
              rule.rule.id,
              rule.rule.inputEntries.map(_.text).toSeq,
              rule.outputs
                .map(out => out.output.name -> unwrap(out.value))
                .toMap
            )
          )
        EvalResult(log.rootEntry.id, rules, maybeError)
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
      results: RunResults
  ): ZIO[Console, EvalException, DmnEvalResult] = {
    for {
      logEntries <- auditLogRef.get
      RunResults(dmn, runResults) = results
      evalResults <- ZIO
        .fromOption(logEntries.groupBy(_.decisionId).get(dmn.id))
        .orElseFail(
          EvalException(dmn.id, s"There is no DMN width id: ${dmn.id}")
        )
      evalMsg <- UIO(missingRules(evalResults, dmn.ruleIds))
      inputKeys <- UIO(runResults.headOption.toSeq.flatMap(_.inputs.view.keys))

      outputKeys <- UIO(
        evalResults.headOption.toSeq
          .flatMap(_.matchedRules)
          .headOption
          .toSeq
          .flatMap(_.outputs.keys)
      )

      dmnEvalRows <- UIO(runResults.zip(evalResults).map {
        case (
              RunResult(testInputs, _),
              EvalResult(
                status: EvalStatus,
                decisionId: String,
                matchedRules: Seq[MatchedRule],
                failed: Option[EvalError]
              )
            ) =>
          DmnEvalRowResult(
            status,
            decisionId,
            testInputs.view.mapValues(_.toString).toMap,
            matchedRules,
            failed
          )
      })

      dmnEvalResult <-
        UIO(
          DmnEvalResult(
            dmn,
            inputKeys,
            outputKeys,
            dmnEvalRows,
            evalMsg
          )
        )
    } yield dmnEvalResult
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

  private def missingRules(
      evalResults: Seq[EvalResult],
      ruleIds: Seq[String]
  ): EvalMsg = {
    val matchedRuleIds =
      evalResults.flatMap(_.matchedRules.map(_.ruleId)).distinct
    def rowIndex(ruleId: String) =
      s"${ruleIds.indexWhere(_ == ruleId) + 1}: $ruleId"

    ruleIds.filterNot(matchedRuleIds.contains(_)).toList match {
      case Nil =>
        EvalMsg(INFO, "All Rules matched at least ones.")
      case l =>
        EvalMsg(
          WARN,
          s"The following Rules never matched: [${l.map(rowIndex).mkString(", ")}]"
        )
    }
  }

}
