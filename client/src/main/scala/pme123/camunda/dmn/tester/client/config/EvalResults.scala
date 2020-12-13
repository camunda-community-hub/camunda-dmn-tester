package pme123.camunda.dmn.tester.client.config

import pme123.camunda.dmn.tester.shared._
import slinky.core.FunctionalComponent
import slinky.core.WithAttrs.build
import slinky.core.annotations.react
import slinky.core.facade.ReactElement
import slinky.web.html._
import typings.antDesignIcons.components.AntdIcon
import typings.antDesignIconsSvg.mod
import typings.antd.components._
import typings.antd.listMod.{ListLocale, ListProps}
import typings.antd.paginationPaginationMod.PaginationConfig
import typings.antd.tableInterfaceMod.{ColumnGroupType, ColumnType}
import typings.antd.{antdBooleans, antdStrings => aStr}
import typings.csstype.csstypeStrings
import typings.rcTable.interfaceMod.{CellType, RenderedCell}
import typings.react.mod.CSSProperties

import scala.scalajs.js

@react object EvalResultsCard {

  case class Props(
      evalResults: Seq[DmnEvalResult],
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
class TableItem(
    val key: Int,
    val name: String,
    val age: Int,
    val address: String
) extends js.Object

@react object EvalResultsList {

  case class Props(
      evalResults: Seq[DmnEvalResult]
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
            .setRenderItem((evalResult: DmnEvalResult, _) =>
              EvalResultsItem(evalResult)
            )

        )
  }
}

@react object EvalResultsItem {

  case class Props(
      evalResult: DmnEvalResult
  )

  val component: FunctionalComponent[Props] = FunctionalComponent[Props] {
    props =>

      val DmnEvalResult(dmn, evalResults) = props.evalResult
      val rowCreator = createRowCreator(props.evalResult)
      List.Item
        .withKey(dmn.id)
        .className("list-item")(
          section(
            h2(dmn.id),
            Table[TableRow]
              .bordered(true)
              .pagination(antdBooleans.`false`)
              // .scroll(ScrollToFirstRowOnChange().x.setScrollToFirstRowOnChange(true))
              .dataSourceVarargs(rowCreator.resultRows: _*)
              .columnsVarargs(
                //   s"EVALUATED: ${formatStrings(inputs)} -> ${formatStrings("Row Number / Rule ID" +: outputs)}"
                ColumnType[TableRow]
                  .setTitle("")
                  .setDataIndex("icon")
                  .setKey("icon")
                  .setRender((_, row, _) =>
                    RenderedCell[TableRow]()
                      .setChildren(icon(row.status))
                      .setProps(CellType().setRowSpan(row.inputRowSpan))
                    ),
                ColumnGroupType[TableRow](js.Array())
                  .setTitleReactElement("Inputs")
                  .setChildrenVarargs(
                    rowCreator.inputs.map(in =>
                      ColumnType[TableRow]
                        .setTitle(in)
                        .setDataIndex(in)
                        .setKey(in)
                        .setRender((_, row, _) =>
                          RenderedCell[TableRow]()
                            .setChildren(Tooltip.TooltipPropsWithOverlayRefAttributes
                              .titleReactElement(row.inputs(in))(
                                span(
                                  className := "ellipsis-text"
                                )(row.inputs(in))
                              )
                              .build)
                            .setProps(CellType().setRowSpan(row.inputRowSpan))
                        )
                    ): _*
                  ),
                ColumnGroupType[TableRow](js.Array())
                  .setTitleReactElement("Outputs")
                  .setChildrenVarargs(

                    ColumnType[TableRow]
                      .setTitle("Dmn Row")
                      .setDataIndex("DmnRow")
                      .setKey("DmnRow")
                      .setRender((_, row, _) =>
                            build(span(row.dmnRowIndex))
                      ) +:
                    rowCreator.outputs.map(out =>
                        ColumnType[TableRow]
                          .setTitle(out)
                          .setDataIndex(out)
                          .setKey(out)
                          .setRender((_, row, _) => {
                            val value = if(row.outputs.isEmpty) "NO RESULT" else row.outputs(out)
                            Tooltip.TooltipPropsWithOverlayRefAttributes
                              .titleReactElement(value)(
                                span(
                                  className := "ellipsis-text"
                                )(value)
                              )
                              .build
                          }
                          )
                    ): _*
                  )
              )
          )
        )
  }

  private def createRowCreator(dmnEvalResult: DmnEvalResult) = {
    val DmnEvalResult(dmn, entries) = dmnEvalResult
    val inputs = entries.headOption.toSeq.flatMap(_.inputs.keys)
    val outputs = entries.headOption.toSeq
      .flatMap(_.matchedRules)
      .headOption
      .toSeq
      .flatMap(_.outputs.keys)
    RowCreator(entries, inputs, outputs, dmn.ruleIds)
    //   _ <- rowCreator.printResultRow()
    //   _ <- rowCreator.printMissingRules()
  }

  private def icon(evalStatus: EvalStatus): ReactElement =
    (evalStatus match {
      case EvalStatus.ERROR =>
        AntdIcon(mod.StopTwoTone).twoToneColor(aStr.red.toString)
      case EvalStatus.WARN =>
        AntdIcon(mod.WarningTwoTone).twoToneColor(aStr.orange.toString)
      case EvalStatus.INFO =>
        AntdIcon(mod.InfoCircleTwoTone).twoToneColor(aStr.green.toString)
    }).style(CSSProperties().setFontSize(20))

}

class TableRow(
    val status: EvalStatus,
    val inputs: Map[String, String],
    val inputRowSpan: Int,
    val dmnRowIndex: Int,
    val outputs: Map[String, String]
) extends js.Object

case class RowCreator(
    evalResults: Seq[EvalResult],
    inputs: Seq[String],
    outputs: Seq[String],
    ruleIds: Seq[String]
) {

  lazy val resultRows: Seq[TableRow] =
    evalResults.sortBy(_.decisionId).flatMap {
      case EvalResult(status, _, inputMap, Nil, _) =>
          Seq(new TableRow(status, inputMap, 1, 0, Map.empty))
      case EvalResult(status, _, inputMap, matchedRules, maybeError) =>
        val outputs = outputMap(matchedRules)
        outputs.zipWithIndex.map { case ((dmnRow, outputMap), index) =>
          new TableRow(status, inputMap, if(index == 0) outputs.size else 0, dmnRow, outputMap)
        }
    }

  private lazy val matchedRuleIds =
    evalResults.flatMap(_.matchedRules.map(_.ruleId)).distinct

  private def outputMap(
      matchedRules: Seq[MatchedRule]
  ) =
    matchedRules.map { case MatchedRule(ruleId, outputMap) =>
      (rowIndex(ruleId), outputMap)
    }.toMap

  private def rowIndex(ruleId: String) =
    ruleIds.indexWhere(_ == ruleId) + 1

}
