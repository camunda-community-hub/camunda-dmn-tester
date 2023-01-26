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
    var children: Seq[TableRow]
) {

  def toParentRow(children: Seq[TableRow]): TableRow = {
    this.children = children
    this
  }

  def toChildRow(): TableRow = {
    this
  }

  private def inOutColSpan(): Int =
    outputMessage.map(_ => 0).getOrElse(1)

  private def inColValue(
      key: String,
      inOutMap: Map[String, String]
  ): String = {
    if (inOutMap.isEmpty)
      outputMessage.getOrElse("-")
    else if (inOutMap.contains(key))
      inOutMap(key)
    else
      outputMessage.getOrElse("x - only needed as Variable for Output.")
  }

  private def outColValue(
      key: String,
      inOutMap: Map[String, TestedValue]
  ): TestedValue =
    if (inOutMap.isEmpty)
      NotTested(outputMessage.getOrElse("-"))
    else inOutMap(key)

}
object TableRow {
  
}

def matchedInputKeys(evalResults: Seq[DmnEvalRowResult]): Seq[String] = {
  evalResults.headOption
    .flatMap(_.matchedRules.headOption.map(_.inputs.map(_._1)))
    .getOrElse(Seq.empty)
}

def matchedOutputKeys(evalResults: Seq[DmnEvalRowResult]): Seq[String] = {
  evalResults.headOption
    .flatMap(_.matchedRules.headOption.map(_.outputs.map(_._1)))
    .getOrElse(Seq.empty)
}

lazy val noMatchingRowsMsg = "NOT FOUND"

