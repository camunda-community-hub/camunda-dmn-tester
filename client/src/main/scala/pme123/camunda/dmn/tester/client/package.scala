package pme123.camunda.dmn.tester

import slinky.core.facade.ReactElement
import typings.antd.components.{Tooltip, Typography}

package object client {

  def textWithTooltip(text: String, tooltip: String): ReactElement =
    Tooltip.TooltipPropsWithOverlayRefAttributes
      .titleReactElement(tooltip)(
        Typography
          .Text(text)
      )
      .build

  def basePathStr(basePath: String): ReactElement = {
    val basePathStr =
      if (basePath.length > 40) ".." + basePath.takeRight(40) else basePath
    textWithTooltip(basePathStr, basePath)
  }
}
