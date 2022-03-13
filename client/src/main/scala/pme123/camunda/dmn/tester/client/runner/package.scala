package pme123.camunda.dmn.tester.client

import pme123.camunda.dmn.tester.shared.{DmnEvalRowResult, TestedValue}
import typings.rcTable.interfaceMod.RenderedCell

package object runner {

  val testInputsKey = "testInputs"
  val variablesKey = "variables"

  def renderTextCell(text: String): RenderedCell[TableRow] = {
    RenderedCell[TableRow]()
      .setChildren(
        textWithTooltip(text, text)
      )
  }

  def matchedInputKeys(evalResults: Seq[DmnEvalRowResult]) = {
    evalResults.headOption
      .flatMap(_.matchedRules.headOption.map(_.inputs.map(_._1)))
      .getOrElse(Seq.empty)
  }

  def matchedOutputKeys(evalResults: Seq[DmnEvalRowResult]) = {
    evalResults.headOption
      .flatMap(_.matchedRules.headOption.map(_.outputs.map(_._1)))
      .getOrElse(Seq.empty)
  }
}
