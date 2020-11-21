package pme123.camunda.dmn.tester

import org.camunda.dmn.Audit._
import org.camunda.dmn.parser.{ParsedInput, ParsedOutput, ParsedRule}
import org.camunda.feel.syntaxtree.{ConstBool, ParsedExpression, ValString}
import zio.test.Assertion.equalTo
import zio.test.{DefaultRunnableSpec, assert, suite, testM}
import zio.{Ref, UIO, ZIO}

object AuditLoggerTest extends DefaultRunnableSpec {

  private def decision(index: Int) = DecisionTableEvaluationResult(
    List(
      EvaluatedInput(
        ParsedInput(
          "p1Id",
          "input1",
          ParsedExpression(ConstBool(true), "true")
        ),
        ValString("hello")
      )
    ),
    List(
      EvaluatedRule(
        ParsedRule(
          s"rule$index",
          List.empty,
          List.empty
        ),
        List(
          EvaluatedOutput(
            ParsedOutput("out1", "Out1", "Out 1", Some("hello"), None),
            ValString("hello")
          )
        )
      )
    ),
    ValString("ok")
  )
  def eval(index: Int, auditLogger: AuditLogger) =
    auditLogger.onEval(
      AuditLog(
        null,
        List(
          AuditLogEntry(
            s"id$index",
            "nameOne",
            null,
            decision(index)
          )
        )
      )
    )
  def spec =
    suite("AuditLogSpex")(
      testM("eval Log and check result") {
        for {
          auditLogRef <- Ref.make(Seq.empty[EvalResult])
          auditLogger <- UIO(
            AuditLogger(auditLogRef)
          )
          _ <- ZIO.foreach_(1 to 10) { i =>
            ZIO(eval(i, auditLogger))
          }
          evalResult <- auditLogRef.get
        } yield assert(evalResult.size)(
          equalTo(10)
        ) //&& if you run it sequential this works only
        //assert(evalResult.head.matchedRules.head.ruleId)(equalTo("rule1"))
      }
    )

}
