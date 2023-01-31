package pme123.camunda.dmn.tester.client

import pme123.camunda.dmn.tester.shared.*

import scala.scalajs.js

case class TableItem(
    key: Int,
    name: String,
    age: Int,
    address: String
)

case class TableRow(
    key: String,
    status: EvalStatus,
    testInputs: Map[String, Any],
    dmnRowIndex: TestedValue,
    inputs: Seq[(String, String)],
    outputs: Seq[(String, TestedValue)],
    outputMessage: Option[String],
    children: Seq[TableRow]
) {

  def toParentRow(children: Seq[TableRow]): TableRow = {
    copy(children = children)
  }

  def toChildRow(): TableRow = {
    this
  }
}

def matchedInputKeys(evalResults: Seq[DmnEvalRowResult]): Seq[String] = {
  evalResults.headOption
    .flatMap(_.matchedRules.headOption.map(_.inputs.keys.toSeq))
    .getOrElse(Seq.empty)
}

def matchedOutputKeys(evalResults: Seq[DmnEvalRowResult]): Seq[String] = {
  evalResults.headOption
    .flatMap(_.matchedRules.headOption.map(_.outputs.map(_._1)))
    .getOrElse(Seq.empty)
}

lazy val noMatchingRowsMsg = "NOT FOUND"

def maxEvalStatus(rows: Seq[TableRow]): EvalStatus =
  rows.map(_.status).min