package pme123.camunda.dmn.tester

import org.camunda.dmn.Audit
import org.camunda.dmn.Audit.{AuditLogListener, DecisionTableEvaluationResult}
import org.camunda.feel.syntaxtree.{Val, ValError}
import org.camunda.feel.valuemapper.ValueMapper
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

  def printLog(): ZIO[Console, Nothing, Unit] = {
    for {
      logEntries <- auditLogRef.get
      _ <- ZIO.foreach_(logEntries.groupBy(_.decisionId)) {
        case (decisionId, entries) =>
          printDmnLog(decisionId, entries)
      }
    } yield ()
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

  private def printDmnLog(decisionId: String, entries: Seq[EvalResult]) = {
    for {
      inputs <- UIO(entries.headOption.toSeq.flatMap(_.inputs.keys).sorted)
      outputs <- UIO(
        entries.headOption.toSeq
          .flatMap(_.matchedRules)
          .headOption
          .toSeq
          .flatMap(_.outputs.keys)
          .sorted
      )
      _ <- console.putStrLn(s"*" * 100)
      _ <- console.putStrLn(s"DMN: $decisionId")
      _ <- console.putStrLn(
        s"EVALUATED: ${formatStrings(inputs)} -> ${formatStrings("Rule ID" +: outputs)}"
      )
      _ <- RowPrinter(entries, inputs, outputs).printResultRow()
    } yield ()

  }

}

case class RowPrinter(
    evalResults: Seq[EvalResult],
    inputs: Seq[String],
    outputs: Seq[String]
) {

  def printResultRow(): URIO[Console, Unit] =
    ZIO.foreach_(evalResults.sortBy(_.decisionId)) {
      case EvalResult(_, inputMap, Nil, None) =>
        console.putStrLn(
          scala.Console.YELLOW +
            s"WARN:      ${formatInputMap(inputMap)} -> NO RESULT" +
            scala.Console.RESET
        )

      case EvalResult(_, inputMap, matchedRules, Some(EvalError(msg))) =>
        val inputStr = s"ERROR:     ${formatInputMap(inputMap)} -> "
        console.putStrLnErr(
          s"""$inputStr${formatOutputMap(matchedRules, inputStr.length)}
            | >>> ${msg.split("\\.").head}""".stripMargin
        )
      case EvalResult(_, inputMap, matchedRules, _) =>
        val inputStr = s"INFO:      ${formatInputMap(inputMap)} -> "
        console.putStrLn(
          s"$inputStr${formatOutputMap(matchedRules, inputStr.length)}"
        )
    }

  private def formatInputMap(inputMap: Map[String, Any]) =
    formatStrings(inputs.map(k => inputMap(k).toString))

  private def formatOutputMap(
      matchedRules: Seq[MatchedRule],
      inputLength: Int
  ) =
    matchedRules
      .map { case MatchedRule(ruleId, outputMap) =>
        s"${formatStrings(ruleId +: outputs.map(k => outputMap(k)))}"
      }
      .mkString(s"\n${"-" * inputLength}")

}

case class EvalResult(
    decisionId: String,
    inputs: Map[String, String],
    matchedRules: Seq[MatchedRule],
    failed: Option[EvalError] = None
)

object EvalResult {
  def failed(errorMsg: String): EvalResult =
    EvalResult("test", Map.empty, Seq.empty, Some(EvalError(errorMsg)))

  def successSingle(value: Any): EvalResult =
    EvalResult(
      "test",
      Map.empty,
      Seq(MatchedRule("someRule", Map("single" -> value.toString)))
    )

  def successMap(resultMap: Map[String, Any]): EvalResult =
    EvalResult(
      "test",
      Map.empty,
      Seq(MatchedRule("someRule", resultMap.view.mapValues(_.toString).toMap))
    )

  lazy val noResult: EvalResult =
    EvalResult("test", Map.empty, Seq.empty)
}
case class MatchedRule(ruleId: String, outputs: Map[String, String])
case class EvalError(msg: String)
