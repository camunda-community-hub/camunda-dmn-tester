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

  /**
   * this removes all dependent Decisions - so we can Unit Test it.
   */
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
      (hitPolicy, rules) = hitPolicyAndRules(decision)
      dmnEvalRows <- ZIO.foreach(allInputs) { inputMap =>
        for {
          context <- UIO(EvalContext(parsedDmn, inputMap, decision))
          evalRow <- evalDecisionRow(decision, context, rules)
        } yield evalRow
      }
      matchedRules = dmnEvalRows.flatMap(_.matchedRules)
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
                               context: EvalContext,
                               rules: Seq[DmnRule]
                             ): ZIO[Any, EvalException, DmnEvalRowResult] =
    for {
      _ <- UIO(
        engine.decisionEval
          .eval(parsedDecision, context) // this is not used - only the AuditLog from the context
      )
      evalResult <- ZIO
        .fromOption(evalResult(AuditLog(context.dmn, context.auditLog.toList), rules, context.variables))
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


  private def evalResult(log: AuditLog, rules: Seq[DmnRule], inputMap: Map[String, Any]) = {
    def rowIndex(ruleId: String) =
      rules.find(_.ruleId == ruleId).map(r => checkIndex(r.index, inputMap)).getOrElse(TestFailure(s"No Rule ID $ruleId found!"))

    Seq(log.rootEntry.result).collectFirst {
      case DecisionTableEvaluationResult(_, matchedRules, result) =>
        val maybeError = Seq(result).collectFirst { case ValError(msg) =>
          EvalError(msg)
        }
        val rules = matchedRules
          .map(rule => {

            val testedIndex = rowIndex(rule.rule.id)
            MatchedRule(
              rule.rule.id,
              testedIndex,
              rule.rule.inputEntries.map(_.text).toSeq,
              checkOutputs(inputMap, testedIndex, rule.outputs
                .map(out => out.output.name -> unwrap(out.value))
                .toMap)
            )
          }
          )
        EvalResult(log.rootEntry.id, rules, maybeError)
    }
  }

  private def checkOutputs(inputMap: Map[String, Any],
                           rowIndex: TestedValue,
                           outputs: Map[String, String]) = {
    if (rowIndex.isError)
      outputs.map {
        case (k, v) =>
          k -> NotTested(v)
      }
    else
      outputs.map {
        case (k, v) =>
          val maybeTestCase = dmnConfig.findTestCase(inputMap.view.mapValues(_.toString).toMap)
          k -> maybeTestCase
            .map(_.checkOut(rowIndex.intValue, k, v))
            .getOrElse(NotTested(v))
      }
  }

  private def checkIndex(rowIndex: Int, inputMap: Map[String, Any]) = {
    val maybeTestCase = dmnConfig.findTestCase(inputMap.view.mapValues(_.toString).toMap)
    maybeTestCase
      .map(_.checkIndex(rowIndex))
      .getOrElse(NotTested(rowIndex.toString))
  }

  private def unwrap(value: Val): String =
    ValueMapper.defaultValueMapper
      .unpackVal(value) match {
      case Some(seq: Seq[_]) => seq.mkString("[", ", ", "]")
      case Some(value) => value.toString
      case None => "NO VALUE"
      case value => s"$value"
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
