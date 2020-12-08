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

import scala.scalajs.js

object components {

  @react object TList {

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
                  "There's nothing to do :(".asInstanceOf[ReactElement]
                )
              )
              .setRenderItem((config: DmnConfig, _) =>
                TItem(config, true, onConfigToggle)
              )
              .setPagination(
                PaginationConfig()
                  .setPosition(aStr.bottom)
                  .setPageSize(10)
              )
          )
    }
  }

  @react object TItem {

    case class Props(
        config: DmnConfig,
        isActive: Boolean,
        onConfigToggle: DmnConfig => Unit
    )

    val component: FunctionalComponent[Props] = FunctionalComponent[Props] {
      props =>
        val Props(config, isActive, onConfigToggle) = props
        List.Item
          .withKey(config.decisionId)
          .className("list-item")
          .actions(
            js.Array(
              Tooltip.TooltipPropsWithOverlayRefAttributes
                .titleReactElement(
                  if (isActive)
                    "Mark as inactive"
                  else "Mark as active"
                )(
                  Switch
                    .checkedChildren(AntdIcon(CheckOutlined))
                    .unCheckedChildren(AntdIcon(CloseOutlined))
                    .onChange((_, _) => onConfigToggle(config))
                    .defaultChecked(isActive)
                )
            )
          )(
            div(
              className := "config-item"
            )(
              Tag(config.decisionId)
                .color(aStr.cyan)
                .className("config-tag")
            )
          )
    }
  }

}
