package pme123.camunda.dmn.tester.server.runner

import org.camunda.dmn.Audit._
import org.camunda.dmn.DmnEngine
import org.camunda.dmn.DmnEngine.EvalContext
import org.camunda.dmn.parser._
import org.camunda.feel.syntaxtree._
import org.camunda.feel.valuemapper.ValueMapper
import pme123.camunda.dmn.tester.server.runner.DmnExtractor.createDmnTables
import pme123.camunda.dmn.tester.shared.HandledTesterException.EvalException
import pme123.camunda.dmn.tester.shared._
import zio.{IO, ZIO}

import java.time.LocalDateTime
import java.time.format.{DateTimeFormatter, FormatStyle}

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
      dmnTables = createDmnTables(dmnConfig, decision)
      dmnEvalRows <- ZIO.foreach(allInputs) { inputMap =>
        for {
          context <- ZIO.succeed(EvalContext(parsedDmn, inputMap, decision))
          evalRow <- evalDecisionRow(decision, context, dmnTables)
        } yield evalRow
      }
      matchedRulesPerTable = dmnEvalRows.flatMap(_.matchedRulesPerTable)
      missingRs = missingRules(
        matchedRulesPerTable,
        dmnTables.mainTable.ruleRows
      )
      outputKeys = matchedRulesPerTable.head.outputKeys
    } yield DmnEvalResult(
      dmnTables,
      inputKeys,
      outputKeys,
      dmnEvalRows,
      missingRs
    )
  }

  private def evalDecisionRow(
      parsedDecision: ParsedDecision,
      context: EvalContext,
      dmnTables: AllDmnTables
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
        .succeed(
          evalResult(
            AuditLog(context.dmn, context.auditLog.toList),
            context.variables,
            dmnTables
          )
        )
    } yield DmnEvalRowResult(
      evalResult.status,
      context.variables.view
        .mapValues(v => if (v == null) "null" else v.toString)
        .toMap,
      evalResult.matchedRules,
      evalResult.failed
    )
  }

  private def evalResult(
      log: AuditLog,
      inputMap: Map[String, Any],
      dmnTables: AllDmnTables
  ) = {

    def matchedInputs(ruleId: String, rules: Seq[DmnRule]) = {
      val ins = rules
        .filter(_.ruleId == ruleId)
        .map(_.inputs)
      ins.headOption.getOrElse(Seq.empty)
    }

    val matchedRulesPerTable: Seq[MatchedRulesPerTable] = dmnTables.tables.map {
      case DmnTable(
            decisionId,
            _,
            _,
            _,
            _,
            _,
            ruleRows
          ) =>
        val isMainTable = dmnTables.isMainTable(decisionId)
        // there must be exactly one LogEntry for each table.
        val maybeLogEntry = log.entries.find(_.id == decisionId)
        val matchedAndErrors =
          maybeLogEntry.flatMap { logEntry =>
            val r =
              Seq(logEntry.result).collectFirst {
                case DecisionTableEvaluationResult(
                _,
                matchedRules,
                result
                ) =>
                  val maybeError: Option[EvalError] = Seq(result).collectFirst {
                    case ValError(msg) =>
                      EvalError(msg)
                  }
                  val matchedR: Seq[MatchedRule] = matchedRules
                    .groupBy(_.rule.id)
                    .map { case ruleId -> mRules => // there must be exactly one MatchedRule
                      if (mRules.size > 1)
                        println(s"UNEXPECTED NR OF RULES: $mRules")

                      val rowIndex =
                        ruleRows
                          .find(_.ruleId == ruleId)
                          .map(r =>
                            if (isMainTable) checkIndex(r.index, inputMap)
                            else NotTested(r.index.toString)
                          )
                          .getOrElse(TestFailure(s"No Rule ID $ruleId found!"))

                      val matchedIns: Seq[(String, String)] =
                        matchedInputs(ruleId, ruleRows)

                      val outputs = mRules
                        .flatMap(_.outputs)
                        .map(out => out.output.name -> unwrapOutput(out.value))
                        .toMap

                      val resultOuts = {
                        if (rowIndex.isError || !isMainTable)
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
                      }.toSeq

                      MatchedRule(
                        ruleId,
                        rowIndex,
                        matchedIns,
                        resultOuts
                      )
                    }
                    .toSeq

                  (matchedR, maybeError)
              }
            r
          }
        MatchedRulesPerTable(
          decisionId,
          matchedAndErrors.toSeq.flatMap(_._1),
          matchedAndErrors.flatMap(_._2)
        )
    }
    EvalResult(matchedRulesPerTable)
  }

  private def checkIndex(rowIndex: Int, inputMap: Map[String, Any]) = {
    val maybeTestCase =
      dmnConfig.findTestCase(inputMap)
    maybeTestCase
      .map(_.checkIndex(rowIndex))
      .getOrElse(NotTested(rowIndex.toString))
  }

  private lazy val dateFormatter =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

  private def unwrapOutput(value: Val): String = {
    val unwrapValue = ValueMapper.defaultValueMapper
      .unpackVal(value)
    def unwrapAny(anyVal: Any): String = anyVal match {
      case Some(seq: Seq[_])    => seq.map(unwrapAny).mkString("[", ", ", "]")
      case Some(value)          => value.toString
      case None                 => "NO VALUE"
      case value: LocalDateTime => dateFormatter.format(value)
      case value                => value.toString
    }
    unwrapAny(unwrapValue)
  }

  private def missingRules(
      matchedRulesByTable: Seq[MatchedRulesPerTable],
      rules: Seq[DmnRule]
  ): Seq[DmnRule] = {
    val matchedRuleIds = matchedRulesByTable
      .filter(_.isForMainTable(dmnConfig.decisionId))
      .flatMap(_.matchedRules)
      .map(_.ruleId)
      .distinct
    rules.filterNot(r => matchedRuleIds.contains(r.ruleId)).toList
  }

  def rowIndex(rule: DmnRule) =
    s"${rule.index}: ${rule.ruleId}"
}
