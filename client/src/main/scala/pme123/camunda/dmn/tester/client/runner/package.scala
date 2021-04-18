package pme123.camunda.dmn.tester.client

import pme123.camunda.dmn.tester.shared.TestedValue
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
}
