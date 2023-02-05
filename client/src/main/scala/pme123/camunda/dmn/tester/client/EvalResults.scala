package pme123.camunda.dmn.tester.client

import pme123.camunda.dmn.tester.shared.*

case class ResultTableRow(
    key: String,
    status: EvalStatus,
    testInputs: Map[String, Any],
    matchedRulesPerTable: Seq[MatchedRulesPerTable],
    outputMessage: Option[String],
) {

  lazy val mainMatchedRules: Seq[MatchedRule] =
    matchedRulesPerTable.headOption.toSeq
      .flatMap(_.matchedRules)

  lazy val mainDecisionId: String =
    matchedRulesPerTable.headOption
      .map(_.decisionId)
      .getOrElse("NO DECISIONS!")

  lazy val mainIndex: TestedValue =
    matchedRulesPerTable.headOption
      .flatMap(_.matchedRules.headOption)
      .map(_.rowIndex)
      .getOrElse(TestFailure("-9999")) // SHOULD NOT HAPPEN!

  lazy val mainInputs: Seq[(String, String)] =
    matchedRulesPerTable.headOption
      .flatMap(_.matchedRules.headOption)
      .toSeq
      .flatMap(_.inputs)

  lazy val mainInputKeys: Seq[String] =
    mainInputs
      .map(_._1)

  lazy val mainOutputs: Seq[(String, TestedValue)] =
    matchedRulesPerTable.headOption
      .flatMap(_.matchedRules.headOption)
      .toSeq
      .flatMap(_.outputs)

  lazy val mainOutputKeys: Seq[String] =
    mainOutputs
      .map(_._1)

  lazy val hasRequiredTables: Boolean =
    matchedRulesPerTable.size > 1

  lazy val tableWithMatchedRules: Option[MatchedRulesPerTable] =
    matchedRulesPerTable.find(_.matchedRules.nonEmpty)
}

lazy val noMatchingRowsMsg = "NOT FOUND"

def maxEvalStatus(rows: Seq[ResultTableRow]): EvalStatus =
  rows.map(_.status).min
