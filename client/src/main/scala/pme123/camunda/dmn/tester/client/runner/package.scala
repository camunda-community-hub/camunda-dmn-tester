package pme123.camunda.dmn.tester.client

import typings.rcTable.interfaceMod.RenderedCell

package object runner {

  def renderTextCell(text: String): RenderedCell[TableRow] = {
    RenderedCell[TableRow]()
      .setChildren(
        textWithTooltip(text, text)
      )
  }
}
