package pme123.camunda.dmn.tester

import slinky.core.TagMod
import slinky.core.facade.ReactElement
import typings.antDesignIcons.components.AntdIcon
import typings.antDesignIconsSvg.typesMod.IconDefinition
import typings.antd.antdStrings.{circle, dashed, primary}
import typings.antd.components.{Button, Col, Tooltip, Typography}

package object client {

  def withTooltip(tooltip: String, reactElement: ReactElement): ReactElement =
    Tooltip.TooltipPropsWithOverlayRefAttributes
      .titleReactElement(tooltip)(
        reactElement
      )
      .build

  def textWithTooltip(text: String, tooltip: String): ReactElement =
    withTooltip(
      tooltip,
      Typography
        .Text(text)
    )

  def buttonWithTooltip(
      icon: IconDefinition,
      tooltip: String,
      onClick: () => Unit
  ): ReactElement =
    withTooltip(
      tooltip,
      Button
        .`type`(primary)
        .shape(circle)
        .icon(AntdIcon(icon))
        .onClick(_ => onClick())
    )

  def buttonWithTextTooltip(
      icon: IconDefinition,
      text: String,
      tooltip: String,
      onClick: () => Unit
  ): ReactElement =
    withTooltip(tooltip,
        Button
          .`type`(dashed)
          .icon(AntdIcon(icon))
          .block(true)
          .onClick(_ => onClick())(text)
      )

  def iconWithTooltip(
      icon: IconDefinition,
      tooltip: String,
      onClick: () => Unit
  ): ReactElement =
    Tooltip.TooltipPropsWithOverlayRefAttributes
      .titleReactElement(tooltip)(
        AntdIcon(icon)
          .onClick(_ => onClick())
      )
      .build

  def basePathStr(basePath: String): ReactElement = {
    val basePathStr =
      if (basePath.length > 40) ".." + basePath.takeRight(40) else basePath
    textWithTooltip(basePathStr, basePath)
  }

  def col(tagMod: TagMod[slinky.web.html.div.tag.type]) =
    Col
      .xs(23)
      .sm(23)
      .md(21)
      .lg(20)
      .xl(18)(
        tagMod
      )
}
