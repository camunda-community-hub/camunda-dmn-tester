package pme123.camunda.dmn.tester.server.runner

import org.camunda.dmn.Audit.{AuditLog, DecisionTableEvaluationResult}
import org.camunda.dmn.DmnEngine
import org.camunda.dmn.DmnEngine.EvalContext
import org.camunda.dmn.parser.{ParsedDecision, ParsedDecisionTable, ParsedDmn, ParsedRule}
import org.camunda.feel.syntaxtree.{Val, ValError}
import org.camunda.feel.valuemapper.ValueMapper
import pme123.camunda.dmn.tester.shared.HandledTesterException.EvalException
import pme123.camunda.dmn.tester.shared._
import zio.{IO, UIO, ZIO}

case class DmnTableEngine(
    parsedDmn: ParsedDmn,
    dmnConfig: DmnConfig,
    engine: DmnEngine = new DmnEngine()
) {
  val DmnConfig(decisionId, data, dmnPath, _) = dmnConfig

  lazy val pureDecision: IO[EvalException, ParsedDecision] =
    parsedDmn.decisionsById
      .get(decisionId)
      .map { decision =>
        UIO(
          decision
            .copy(requiredBkms = Seq.empty, requiredDecisions = Seq.empty)
        )
      }
      .getOrElse(
        ZIO.fail(
          EvalException(decisionId, s"No decision found with id '$decisionId'")
        )
      )

  def evalDecision(
      allInputs: Seq[Map[String, Any]]
  ): ZIO[Any, EvalException, DmnEvalResult] = {
    for {
      decision <- pureDecision
      dmnEvalRows <- ZIO.foreach(allInputs) { inputMap =>
        for {
          context <- UIO(EvalContext(parsedDmn, inputMap, decision))
          evalRow <- evalDecisionRow(decision, context)
        } yield evalRow
      }
      matchedRules = dmnEvalRows.flatMap(_.matchedRules)
      (hitPolicy, rules) = hitPolicyAndRules(decision)
      dmn = Dmn(
        dmnConfig.decisionId,
        HitPolicy(hitPolicy.toString),
        dmnConfig,
        rules
      )
      missingRs = missingRules(matchedRules, rules)
      inputKeys = dmnEvalRows.headOption.toSeq.flatMap(_.testInputs.keys)
      outputKeys = matchedRules.headOption.toSeq.flatMap(_.outputs.keys)
    } yield DmnEvalResult(
      dmn,
      inputKeys,
      outputKeys,
      dmnEvalRows,
      missingRs
    )
  }

  private def evalDecisionRow(
      parsedDecision: ParsedDecision,
      context: EvalContext
  ): ZIO[Any, EvalException, DmnEvalRowResult] =
    for {
      _ <- UIO(
        engine.decisionEval
          .eval(parsedDecision, context) // this is not used - only the AuditLog from the context
      )
      evalResult <- ZIO
        .fromOption(evalResult(AuditLog(context.dmn, context.auditLog.toList)))
        .orElseFail(EvalException(decisionId, "No Evaluation Result"))
    } yield
      DmnEvalRowResult(
        evalResult.status,
        decisionId,
        context.variables.view.mapValues(_.toString).toMap,
        evalResult.matchedRules,
        evalResult.failed
      )

  private def hitPolicyAndRules(decision: ParsedDecision) = {
    decision.logic match {
      case ParsedDecisionTable(_, _, rules, hitPolicy, _) =>
        hitPolicy -> rules.zipWithIndex.map {
          case (ParsedRule(id, inputs, outputs), index) =>
            DmnRule(
              index + 1,
              id,
              inputs.map(_.text).toSeq,
              outputs.map(_._2.text).toSeq
            )
        }.toSeq
      case other => s"No ParsedDecisionTable: $other" -> Seq.empty
    }
  }

  private def evalResult(log: AuditLog) =
    Seq(log.rootEntry.result).collectFirst {
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

  private def unwrap(value: Val): String =
    ValueMapper.defaultValueMapper
      .unpackVal(value) match {
      case Some(seq: Seq[_]) => seq.mkString("[", ", ", "]")
      case Some(value)       => value.toString
      case None              => "NO VALUE"
      case value             => s"$value"
    }

  private def missingRules(
      matchedRules: Seq[MatchedRule],
      rules: Seq[DmnRule]
  ): Seq[DmnRule] = {
    val matchedRuleIds = matchedRules.map(_.ruleId).distinct
    rules.filterNot(r => matchedRuleIds.contains(r.ruleId)).toList
  }

  def rowIndex(rule: DmnRule) =
    s"${rule.index}: ${rule.ruleId}"
}
