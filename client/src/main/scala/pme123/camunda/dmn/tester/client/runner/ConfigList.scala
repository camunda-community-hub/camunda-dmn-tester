package pme123.camunda.dmn.tester.client.runner

import pme123.camunda.dmn.tester.client.runner.ConfigItem.activeCheck
import pme123.camunda.dmn.tester.shared.DmnConfig
import slinky.core.FunctionalComponent
import slinky.core.annotations.react
import slinky.core.facade.Hooks.useState
import slinky.core.facade.{Fragment, ReactElement}
import slinky.web.html._
import typings.antDesignIcons.components.AntdIcon
import typings.antDesignIconsSvg.mod.{CheckOutlined, CloseOutlined}
import typings.antd.components._
import typings.antd.listMod.{ListLocale, ListProps}
import typings.antd.paginationPaginationMod.PaginationConfig
import typings.antd.{antdStrings => aStr}

import scala.scalajs.js
import scala.scalajs.js.Dynamic.literal

@react object ConfigCard {

  case class Props(
      configs: Seq[DmnConfig],
      isLoaded: Boolean,
      maybeError: Option[String],
      setConfigs: Seq[DmnConfig] => Unit
  )

  val component: FunctionalComponent[Props] = FunctionalComponent[Props] {
    props =>
      val (isActive, setIsActive) = useState(false)
      val Props(configs, isLoaded, maybeError, setConfigs) = props

      lazy val handleConfigToggle = { (config: DmnConfig) =>
        val newCF = config.copy(isActive = !config.isActive)
        setConfigs(configs.map {
          case c if c.decisionId == config.decisionId =>
            newCF
          case c => c
        })
      }

      Card
        .title(
          Fragment(
            "2. Select the DMN Configurations you want to test.",
            div(style := literal(textAlign = "right", marginRight = 10))(
              activeCheck(
                isActive = isActive,
                active => {
                  setIsActive(active)
                  setConfigs(configs.map(_.copy(isActive = active)))
                }
              )
            )
          )
        )(
          (maybeError, isLoaded) match {
            case (Some(msg), _) =>
              Alert
                .message(
                  s"Error: The DMN Configurations could not be loaded. (is the path ok?)"
                )
                .`type`(aStr.error)
                .showIcon(true)
            case (_, false) =>
              Spin
                .size(aStr.default)
                .spinning(true)(
                  Alert
                    .message("Loading Configs")
                    .`type`(aStr.info)
                    .showIcon(true)
                )
            case _ =>
              ConfigList(configs, handleConfigToggle)
          }
        )
  }
}

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
                Empty().description("There are no DMN configurations:(").build
              )
            )
            .setRenderItem((config: DmnConfig, _) =>
              ConfigItem(config, onConfigToggle)
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
