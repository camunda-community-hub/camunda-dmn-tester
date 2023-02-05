package pme123.camunda.dmn.tester.server.runner

import org.camunda.dmn.parser._
import org.camunda.feel.syntaxtree.{ParsedExpression => FeelParsedExpression}
import pme123.camunda.dmn.tester.shared._

object DmnExtractor {

  def createDmnTables(
      dmnConfig: DmnConfig,
      decision: ParsedDecision
  ): AllDmnTables = {
    AllDmnTables(
      dmnConfig,
      // the required ParsedDecision are double!? -> distinct
      (decision +: decision.requiredDecisions.toSeq.distinct).collect {
        case ParsedDecision(
              decisionId,
              name,
              ParsedDecisionTable(
                inputs,
                outputs,
                rules,
                hitPolicy,
                aggregator
              ),
              _,
              _,
              _,
              _
            ) =>
          val inputCols = inputs.collect {
            case ParsedInput(
                  _,
                  name,
                  FeelExpression(FeelParsedExpression(_, feelExprText))
                ) =>
              InputColumn(name, feelExprText)
          }.toSeq
          val outputCols = outputs.collect {
            case ParsedOutput(_, name, _, value, _) =>
              OutputColumn(name, value)
          }.toSeq
          val ruleRows = rules.zipWithIndex.map {
            case (
                  ParsedRule(id, inputs: Iterable[ParsedExpression], outputs),
                  index
                ) =>
              DmnRule(
                index + 1,
                id,
                inputCols
                  .map(_.name)
                  .zip(inputs.toSeq)
                  .map(i => i._1 -> extractFrom(i._2)),
                outputs.map(o => o._1 -> extractFrom(o._2)).toSeq
              )
          }.toSeq
          DmnTable(
            decisionId,
            name,
            HitPolicy(hitPolicy.name()),
            Option(aggregator).map(a => Aggregator(a.name())),
            inputCols,
            outputCols,
            ruleRows
          )
      }
    )
  }

  private def extractFrom(expr: ParsedExpression) = expr match {
    case ExpressionFailure(failure) => failure
    case FeelExpression(expr) =>
      expr.text
    case EmptyExpression => ""
  }
}
