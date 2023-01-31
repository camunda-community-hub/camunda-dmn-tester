package pme123.camunda.dmn.tester.server.runner

import org.camunda.dmn.Audit._
import org.camunda.dmn.DmnEngine
import org.camunda.dmn.DmnEngine.EvalContext
import org.camunda.dmn.parser._
import org.camunda.feel.syntaxtree.{
  Val,
  ValError,
  ParsedExpression => FeelParsedExpression
}
import org.camunda.feel.valuemapper.ValueMapper
import pme123.camunda.dmn.tester.shared.HandledTesterException.EvalException
import pme123.camunda.dmn.tester.shared._
import zio.{IO, ZIO}

case class DmnTableEngine(
    parsedDmn: ParsedDmn,
    dmnConfig: DmnConfig,
    engine: DmnEngine = new DmnEngine()
) {
  val DmnConfig(decisionId, _, dmnPath, _, testUnit) = dmnConfig

  /** If `testUnit` is set: removes all dependent Decisions - so we can Unit
    * Test it.
    */
  private lazy val pureDecision: IO[EvalException, ParsedDecision] =
    parsedDmn.decisionsById
      .get(decisionId)
      .map { decision =>
        ZIO.succeed(
          if (testUnit)
            decision
              .copy(requiredBkms = Seq.empty, requiredDecisions = Seq.empty)
          else
            decision
        )
      }
      .getOrElse(
        ZIO.fail(
          EvalException(dmnConfig, s"No decision found with id '$decisionId'")
        )
      )

  def evalDecision(
      inputKeys: Seq[String],
      allInputs: Seq[Map[String, Any]]
  ): ZIO[Any, EvalException, DmnEvalResult] = {
    for {
      decision <- pureDecision
      (hitPolicy, rules) = hitPolicyAndRules(decision)
      dmnEvalRows <- ZIO.foreach(allInputs) { inputMap =>
        for {
          context <- ZIO.succeed(EvalContext(parsedDmn, inputMap, decision))
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
      outputKeys = matchedRules.headOption.toSeq.flatMap(_.outputs.map(_._1))
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
  ): ZIO[Any, EvalException, DmnEvalRowResult] = {
    for {
      _ <- ZIO.succeed(
        engine.decisionEval
          .eval(
            parsedDecision,
            context
          ) // this is not used - only the AuditLog from the context
      )
      evalResult <- ZIO
        .fromOption(
          evalResult(
            AuditLog(context.dmn, context.auditLog.toList),
            rules,
            context.variables
          )
        )
        .orElseFail(EvalException(dmnConfig, "No Evaluation Result"))
    } yield DmnEvalRowResult(
      evalResult.status,
      decisionId,
      context.variables.view
        .mapValues(v => if (v == null) "null" else v.toString)
        .toMap,
      evalResult.matchedRules,
      evalResult.failed
    )
  }

  private def hitPolicyAndRules(decision: ParsedDecision) = {
    decision.logic match {
      case ParsedDecisionTable(_, _, rules, hitPolicy, _) =>
        hitPolicy -> rules.zipWithIndex.map {
          case (
                ParsedRule(id, inputs: Iterable[ParsedExpression], outputs),
                index
              ) =>
            DmnRule(
              index + 1,
              id,
              inputs.map(extractFrom).toSeq,
              outputs.map(o => extractFrom(o._2)).toSeq
            )
        }.toSeq
      case other => s"No ParsedDecisionTable: $other" -> Seq.empty
    }
  }
  private def extractFrom(expr: ParsedExpression) = expr match {
    case ExpressionFailure(failure) => failure
    case FeelExpression(expr) =>
      expr.text
    case EmptyExpression => ""
  }

  private def evalResult(
      log: AuditLog,
      rules: Seq[DmnRule],
      inputMap: Map[String, Any]
  ) = {
    def rowIndex(ruleId: String) =
      rules
        .find(_.ruleId == ruleId)
        .map(r => checkIndex(r.index, inputMap))
        .getOrElse(TestFailure(s"No Rule ID $ruleId found!"))

    Seq(log.rootEntry.result).collectFirst {
      case DecisionTableEvaluationResult(_, matchedRules, result) =>
        val maybeError = Seq(result).collectFirst { case ValError(msg) =>
          EvalError(msg)
        }
        val rules = matchedRules
          .map(rule => {
            println(s"RULE: $rule")

            val testedIndex = rowIndex(rule.rule.id)
            MatchedRule(
              rule.rule.id,
              testedIndex,
              log.entries
                .foldLeft(Map.empty[String, String])((result, logEntry) => {
                  result ++ extractInputs(logEntry.result)
                }),
              checkOutputs(
                inputMap,
                testedIndex,
                rule.outputs
                  .map(out => out.output.name -> unwrap(out.value))
                  .toMap
              ).toSeq
            )
          })
        EvalResult(log.rootEntry.id, rules, maybeError)
    }
  }

  private def extractInputs(
      evaluationResult: EvaluationResult
  ) =
    evaluationResult match {
      case DecisionTableEvaluationResult(inputs, _, _) =>
        val ins = inputs
          .map {
            case EvaluatedInput(
                  ParsedInput(
                    _,
                    name,
                    FeelExpression(FeelParsedExpression(_, inputKey))
                  ),
                  value
                ) =>
              (inputKey == name,   name, unwrap(value))
          }
          .filter(in => !in._1 || (in._1 && dmnConfig.inputKeys.contains(in._2)))
          .map(in => (in._2, in._3))
        println(s"INPUTS: $ins")
        ins.toMap
      case SingleEvaluationResult(_) =>
        Map.empty
      case ContextEvaluationResult(entries, _) =>
        entries.toSeq.map { case k -> v =>
          k -> v.toString
        }.toMap
    }

  private def checkOutputs(
      inputMap: Map[String, Any],
      rowIndex: TestedValue,
      outputs: Map[String, String]
  ) = {
    if (rowIndex.isError)
      outputs.map { case (k, v) =>
        k -> NotTested(v)
      }
    else
      outputs.map { case (k, v) =>
        val maybeTestCase =
          dmnConfig.findTestCase(inputMap)
        k -> maybeTestCase
          .map(_.checkOut(rowIndex.intValue, k, v))
          .getOrElse(NotTested(v))
      }
  }

  private def checkIndex(rowIndex: Int, inputMap: Map[String, Any]) = {
    val maybeTestCase =
      dmnConfig.findTestCase(inputMap)
    maybeTestCase
      .map(_.checkIndex(rowIndex))
      .getOrElse(NotTested(rowIndex.toString))
  }

  private def unwrap(value: Val): String = {
    val unwrapValue = ValueMapper.defaultValueMapper
      .unpackVal(value)
    unwrapValue match {
      case Some(seq: Seq[_]) => seq.mkString("[", ", ", "]")
      case Some(value)       => value.toString
      case None              => "NO VALUE"
      case value             => s"$value"
    }
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
