package pme123.camunda.dmn.tester.client.config

import pme123.camunda.dmn.tester.client.config.ConfigItem.activeCheck
import pme123.camunda.dmn.tester.shared.{DmnConfig, EvalResult, EvalStatus}
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

@react object EvalResultsCard {

  case class Props(
      evalResults: Seq[EvalResult],
      isLoaded: Boolean,
      maybeError: Option[String]
  )

  val component: FunctionalComponent[Props] = FunctionalComponent[Props] {
    props =>
      val Props(evalResults, isLoaded, maybeError) = props

      Card
        .title("4. Check the Test Results.")(
          (maybeError, isLoaded) match {
            case (Some(msg), _) =>
              Alert
                .message(
                  s"Error: The DMN EvalResultsurations could not be loaded. (is the path ok?)"
                )
                .`type`(aStr.error)
                .showIcon(true)
            case (_, false) =>
              Spin
                .size(aStr.default)
                .spinning(true)(
                  Alert
                    .message("Loading EvalResultss")
                    .`type`(aStr.info)
                    .showIcon(true)
                )
            case _ =>
              EvalResultsList(evalResults)
          }
        )
  }
}

@react object EvalResultsList {

  case class Props(
      evalResults: Seq[EvalResult]
  )

  val component: FunctionalComponent[Props] = FunctionalComponent[Props] {
    props =>
      val Props(evalResults) = props
      List
        .withProps(
          ListProps()
            .setDataSource(js.Array(evalResults: _*))
            .setLocale(
              ListLocale().setEmptyText(
                "There are no DMN configurations:(".asInstanceOf[ReactElement]
              )
            )
            .setRenderItem((evalResult: EvalResult, _) =>
              EvalResultsItem(evalResult)
            )
            .setPagination(
              PaginationConfig()
                .setPosition(aStr.bottom)
                .setPageSize(10)
            )
        )
  }
}

@react object EvalResultsItem {

  case class Props(
      evalResult: EvalResult
  )

  val component: FunctionalComponent[Props] = FunctionalComponent[Props] {
    props =>
      val Props(evalResult) = props
      List.Item
        .withKey(evalResult.decisionId)
        .className("list-item")(
          div(
            className := "evalResult-item"
          )(
            Tag(s"${evalResult.decisionId}")
              .color(evalResult.status match {
                case EvalStatus.ERROR => aStr.red
                case EvalStatus.WARN  => aStr.orange
                case EvalStatus.INFO  => aStr.green
              })
              .className("evalResult-tag")
          )
        )
  }

}
