package pme123.camunda.dmn.tester.client.config

import pme123.camunda.dmn.tester.shared.DmnConfig
import slinky.core.FunctionalComponent
import slinky.core.annotations.react
import slinky.core.facade.ReactElement
import slinky.web.html._
import typings.antDesignIcons.components.AntdIcon
import typings.antDesignIconsSvg.mod.{CheckOutlined, CloseOutlined}
import typings.antd.components._
import typings.antd.listMod.{ListLocale, ListProps}
import typings.antd.paginationPaginationMod.PaginationConfig
import typings.antd.{antdStrings => aStr}
import typings.csstype.csstypeStrings
import typings.csstype.mod.TextAlignProperty
import typings.react.mod.CSSProperties

import scala.scalajs.js

@react object ConfigList {

  case class Props(
      configs: Seq[DmnConfig],
      onConfigToggle: DmnConfig => Unit
  )

  val component: FunctionalComponent[Props] = FunctionalComponent[Props] {
    props =>
      val Props(configs, onConfigToggle) = props
      List
        .withProps(
          ListProps()
            .setDataSource(js.Array(configs: _*))
            .setLocale(
              ListLocale().setEmptyText(
                "There are no DMN configurations:(".asInstanceOf[ReactElement]
              )
            )
            .setRenderItem((config: DmnConfig, _) =>
              ConfigItem(config, onConfigToggle)
            )
            .setPagination(
              PaginationConfig()
                .setPosition(aStr.bottom)
                .setPageSize(10)
            )
        )
  }
}

@react object ConfigItem {

  case class Props(
      config: DmnConfig,
      onConfigToggle: DmnConfig => Unit
  )

  val component: FunctionalComponent[Props] = FunctionalComponent[Props] {
    props =>
      val Props(config, onConfigToggle) = props
      List.Item
        .withKey(config.decisionId)
        .className("list-item")
        .actions(
          js.Array(
            Tooltip.TooltipPropsWithOverlayRefAttributes
              .titleReactElement(
                if (config.isActive)
                  "Mark as inactive"
                else "Mark as active"
              )(
                activeCheck(config.isActive, _ => onConfigToggle(config))
              )
          )
        )(
          div(
            className := "config-item"
          )(
            Tag(s"${config.decisionId} -> ${config.dmnPath.mkString("/")}")
              .color(if (config.isActive) aStr.blue else aStr.red)
              .className("config-tag")
          )
        )
  }

  def activeCheck(
      isActive: Boolean,
      onConfigToggle: Boolean => Unit
  ) = {
    Switch
      .checkedChildren(AntdIcon(CheckOutlined))
      .unCheckedChildren(AntdIcon(CloseOutlined))
      .onChange((active, _) => onConfigToggle(active))
      .checked(isActive)
  }
}
