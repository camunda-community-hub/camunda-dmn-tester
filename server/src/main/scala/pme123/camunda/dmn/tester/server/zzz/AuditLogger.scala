package pme123.camunda.dmn.tester.server.zzz

import org.camunda.dmn.Audit
import org.camunda.dmn.Audit.{AuditLogListener, DecisionTableEvaluationResult}
import org.camunda.feel.syntaxtree.{Val, ValError}
import org.camunda.feel.valuemapper.ValueMapper
import pme123.camunda.dmn.tester.shared._
import zio._
import zio.console.Console
import pme123.camunda.dmn.tester.shared.EvalStatus._

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
        println(s"ON EVAL: $ins: ${log.rootEntry.id}")
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
      runResults: Seq[RunResults]
  ): ZIO[Console, Nothing, Seq[DmnEvalResult]] = {
    for {
      logEntries <- auditLogRef.get
      entryMap <- UIO(logEntries.groupBy(_.decisionId))
      results <- ZIO.foreach(runResults) { case RunResults(dmn, res) =>
        UIO(
          DmnEvalResult(
            dmn,
            res.map(_.inputs.view.mapValues(_.toString).toMap),
            entryMap.getOrElse(dmn.id, Nil)
          )
        )
      }
    } yield results
  }

  def printLog(
      runResults: Seq[RunResults]
  ): ZIO[Console, NoSuchElementException, Unit] =
    for {
      evalResults <- getDmnEvalResults(runResults)
      _ <- ZIO.foreach_(evalResults) { evalResult =>
        printDmnLog(evalResult)
      }
    } yield ()

  private def unwrap(value: Val): String =
    ValueMapper.defaultValueMapper
      .unpackVal(value) match {
      case Some(seq: Seq[_]) => seq.mkString("[", ", ", "]")
      case Some(value)       => value.toString
      case None              => "NO VALUE"
      case value =>
        s"$value"
    }

  private def printDmnLog(dmnEvalResult: DmnEvalResult) = {
    val DmnEvalResult(dmn, _, entries) = dmnEvalResult
    for {
      inputs <- UIO(entries.headOption.toSeq.flatMap(_.inputs.keys))
      outputs <- UIO(
        entries.headOption.toSeq
          .flatMap(_.matchedRules)
          .headOption
          .toSeq
          .flatMap(_.outputs.keys)
      )
      _ <- console.putStrLn(s"*" * 100)
      _ <- console.putStrLn(s"DMN: ${dmn.id}")
      _ <- console.putStrLn(
        s"EVALUATED: ${formatStrings(inputs)} -> ${formatStrings("Row Number / Rule ID" +: outputs)}"
      )
      rowPrinter <- UIO(RowPrinter(entries, inputs, outputs, dmn.ruleIds))
      _ <- rowPrinter.printResultRow()
      _ <- rowPrinter.printMissingRules()
    } yield ()

  }

}

case class RowPrinter(
    evalResults: Seq[EvalResult],
    inputs: Seq[String],
    outputs: Seq[String],
    ruleIds: Seq[String]
) {

  def printResultRow(): URIO[Console, Unit] =
    ZIO.foreach_(evalResults.sortBy(_.decisionId)) {
      case EvalResult(WARN, _, inputMap, Nil, None) =>
        printWarning(
          s"WARN:      ${formatInputMap(inputMap)} -> NO RESULT"
        )

      case EvalResult(ERROR, _, inputMap, matchedRules, Some(EvalError(msg))) =>
        val inputStr = s"ERROR:     ${formatInputMap(inputMap)} -> "
        printError(
          s"""$inputStr${formatOutputMap(matchedRules, inputStr.length)}
            | >>> ${msg.split("\\.").head}""".stripMargin
        )
      case EvalResult(INFO, _, inputMap, matchedRules, _) =>
        val inputStr = s"INFO:      ${formatInputMap(inputMap)} -> "
        console.putStrLn(
          s"$inputStr${formatOutputMap(matchedRules, inputStr.length)}"
        )
    }

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

  private def formatInputMap(inputMap: Map[String, Any]) =
    formatStrings(inputs.map(k => inputMap(k).toString))

  private def formatOutputMap(
      matchedRules: Seq[MatchedRule],
      inputLength: Int
  ) =
    matchedRules
      .map { case MatchedRule(ruleId, outputMap) =>
        s"${formatStrings(rowIndex(ruleId: String) +: outputs.map(k => outputMap(k)))}"
      }
      .mkString(s"\n${"-" * inputLength}")

  private def rowIndex(ruleId: String) =
    s"${ruleIds.indexWhere(_ == ruleId) + 1}: $ruleId"

}
