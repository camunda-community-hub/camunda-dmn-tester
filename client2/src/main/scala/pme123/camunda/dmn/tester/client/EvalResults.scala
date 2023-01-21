package pme123.camunda.dmn.tester.client

import pme123.camunda.dmn.tester.shared.*

import scala.scalajs.js

class TableItem(
    val key: Int,
    val name: String,
    val age: Int,
    val address: String
) extends js.Object

class TableRow(
    val key: String,
    val status: EvalStatus,
    val testInputs: Map[String, Any],
    var inputRowSpan: Int,
    val dmnRowIndex: TestedValue,
    val inputs: Seq[(String, String)],
    val outputs: Seq[(String, TestedValue)],
    val outputMessage: Option[String],
    var children: Seq[TableRow]
) {

  def totalRowSpan: Double = children.size + inputRowSpan

  def toParentRow(children: Seq[TableRow]): TableRow = {
    this.children = children
    this
  }

  def toChildRow(): TableRow = {
    this.inputRowSpan = 0
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

