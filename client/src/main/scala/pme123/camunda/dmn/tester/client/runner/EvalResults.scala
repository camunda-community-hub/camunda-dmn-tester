package pme123.camunda.dmn.tester.client.runner

import pme123.camunda.dmn.tester.shared.EvalStatus.ERROR
import pme123.camunda.dmn.tester.shared.HandledTesterException.EvalException
import pme123.camunda.dmn.tester.shared.{DmnEvalRowResult, _}
import slinky.core.FunctionalComponent
import slinky.core.WithAttrs.build
import slinky.core.annotations.react
import slinky.core.facade.ReactElement
import slinky.web.html._
import typings.antDesignIcons.components.AntdIcon
import typings.antDesignIconsSvg.mod
import typings.antd.components._
import typings.antd.listMod.{ListLocale, ListProps}
import typings.antd.tableInterfaceMod.{ColumnGroupType, ColumnType}
import typings.antd.{antdBooleans, antdStrings => aStr}
import typings.rcTable.interfaceMod.{CellType, RenderedCell, TableLayout}
import typings.react.mod.CSSProperties

import scala.scalajs.js

@react object EvalResultsCard {

  case class Props(
      evalResults: Seq[Either[EvalException, DmnEvalResult]],
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
                  s"Error: The DMN EvalResults could not be loaded. ($msg)"
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
      evalResults: Seq[Either[EvalException, DmnEvalResult]]
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
                Empty().description("There are no Tests selected:(").build
              )
            )
            .setRenderItem(
              (evalResult: Either[EvalException, DmnEvalResult], _) =>
                EvalResultsItem(evalResult)
            )
        )
  }
}

@react object EvalResultsItem {

  case class Props(
      evalResult: Either[EvalException, DmnEvalResult]
  )

  val component: FunctionalComponent[Props] = FunctionalComponent[Props] {
    case Props(Left(EvalException(decisionId, msg))) =>
      List.Item
        .withKey(decisionId)
        .className("list-item")(
          section(
            h2(Space(icon(ERROR), span(decisionId))),
            pre(msg)
          )
        )
    case Props(Right(er @ DmnEvalResult(dmn, _,_,_, _, evalMsg))) =>
      val rowCreator = RowCreator(er)
      List.Item
        .withKey(dmn.id)
        .className("list-item")(
          section(
            h2(Space(icon(er.maxEvalStatus), span(dmn.id))),
            p(s"Hitpolicy: ${dmn.hitPolicy}"),
            Space(icon(evalMsg.status), evalMsg.msg),
            Table[TableRow]
              .tableLayout(TableLayout.fixed)
              .bordered(true)
              .pagination(antdBooleans.`false`)
              .dataSourceVarargs(rowCreator.resultRows: _*)
              .columnsVarargs(
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
                  .setTitleReactElement("Test Input(s)")
                  .setChildrenVarargs(
                    rowCreator.testInputKeys.map(in =>
                      ColumnType[TableRow]
                        .setTitle(in)
                        .setDataIndex(in)
                        .setEllipsis(true)
                        .setKey(in)
                        .setRender((_, row, _) =>
                          renderTextCell(row.testInputs(in), 1)
                            .setProps(CellType().setRowSpan(row.inputRowSpan))
                        )
                    ): _*
                  ),
                ColumnType[TableRow]
                  .setTitle("Dmn Row")
                  .setEllipsis(true)
                  .setDataIndex("DmnRow")
                  .setKey("DmnRow")
                  .setRender { (_, row, _) =>
                    row.outputMessage match {
                      case Some(msg) =>
                        val colSpan = rowCreator.inputKeys.length + rowCreator.outputKeys.length + 1
                        renderTextCell(msg, colSpan)
                          .setProps(
                            CellType()
                              .setColSpan(colSpan)
                              .setClassName(s"${row.status}-cell")
                          )
                      case _ => renderTextCell(row.dmnRowIndex.toString, 1)
                    }

                  },
                ColumnGroupType[TableRow](js.Array())
                  .setTitleReactElement("Inputs")
                  .setChildrenVarargs(
                    rowCreator.inputKeys.map { in =>
                      ColumnType[TableRow]
                        .setTitle(in)
                        .setEllipsis(true)
                        .setDataIndex(in)
                        .setKey(in)
                        .setRender((_, row, _) => {
                          val value =
                            if (row.inputs.isEmpty)
                              row.outputMessage.getOrElse("-")
                            else row.inputs(in)
                          val colSpan =
                            row.outputMessage.map(_ => 0).getOrElse(1)
                          renderTextCell(value, colSpan)
                            .setProps(
                              CellType()
                                .setColSpan(colSpan)
                            )
                        })
                    }: _*
                  ),
                ColumnGroupType[TableRow](js.Array())
                  .setTitleReactElement("Outputs")
                  .setChildrenVarargs(
                    rowCreator.outputKeys.map { out =>
                    println(s"OUT: $out")
                      ColumnType[TableRow]
                        .setTitle(out)
                        .setEllipsis(true)
                        .setDataIndex(out)
                        .setKey(out)
                        .setRender((_, row, _) => {
                          println(s"out: $out - ${row.outputs}")
                          val value =
                            if (row.outputs.isEmpty)
                              row.outputMessage.getOrElse("-")
                            else row.outputs(out)
                          val colSpan =
                            row.outputMessage.map(_ => 0).getOrElse(1)
                          renderTextCell(value, colSpan)
                            .setProps(
                              CellType()
                                .setColSpan(colSpan)
                            )
                        })
                    }: _*
                  )
              )
          )
        )
  }

  private def renderTextCell(text: String, colSpan: Int) = {
    RenderedCell[TableRow]()
      .setChildren(
        Tooltip.TooltipPropsWithOverlayRefAttributes
          .titleReactElement(text)(
            Typography
              .Text(text)
          )
          .build
      )
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
    val testInputs: Map[String, String],
    val inputRowSpan: Int,
    val dmnRowIndex: Int,
    val inputs: Map[String, String],
    val outputs: Map[String, String],
    val outputMessage: Option[String]
) extends js.Object

case class RowCreator(
    dmnEvalResult: DmnEvalResult
) {
  val DmnEvalResult(dmn, testInputKeys, inputKeys, outputKeys, evalResults, evalMsg) = dmnEvalResult

  lazy val resultRows: Seq[TableRow] =
    evalResults.sortBy(_.decisionId).flatMap {
      case DmnEvalRowResult(status, decisionId,testInputs,Nil, maybeError) =>
        Seq(
          new TableRow(
            status,
            testInputs,
            1,
            0,
            Map.empty,
            Map.empty,
            Some(maybeError.map(_.msg).getOrElse("NOT FOUND"))
          )
        )
      case DmnEvalRowResult(status, _, testInputMap, matchedRules, maybeError) =>
        val errorRow = maybeError
          .map(msg =>
            new TableRow(
              status,
              testInputMap,
              0,
              0,
              Map.empty,
              Map.empty,
              Some(msg.msg)
            )
          )
          .toSeq

        //val outputs = outputMap(matchedRules)
        val rows =
          matchedRules.zipWithIndex.map {
            case (MatchedRule(ruleId, inputMap, outputMap), index) =>
              new TableRow(
                status,
                testInputMap,
                if (index == 0)
                  matchedRules.size +
                    maybeError.map(_ => 1).getOrElse(0) // add an extra row
                else 0,
                rowIndex(ruleId),
                inputMap,
                outputMap,
                None
              )
          }
        rows ++ errorRow
    }

  private lazy val matchedRuleIds =
    evalResults.flatMap(_.matchedRules.map(_.ruleId)).distinct

  private def rowIndex(ruleId: String) =
    dmn.ruleIds.indexWhere(_ == ruleId) + 1


}
