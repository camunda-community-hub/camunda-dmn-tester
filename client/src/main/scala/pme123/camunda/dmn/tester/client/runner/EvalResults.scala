package pme123.camunda.dmn.tester.client.runner

import pme123.camunda.dmn.tester.client.textWithTooltip
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
    case Props(
          Right(
            er @ DmnEvalResult(
              dmn,
              inputKeys,
              outputKeys,
              _,
              _
            )
          )
        ) =>
      val rowCreator = RowCreator(er)

      List.Item
        .withKey(dmn.id)
        .className("list-item")(
          section(
            h2(Space(icon(er.maxEvalStatus), span(dmn.id))),
            p(s"Hitpolicy: ${dmn.hitPolicy}"),
            Table[TableRow]
              .withKey(dmn.id + "Key")
              .defaultExpandAllRows(true)
              .expandIcon(_ => "")
              .tableLayout(TableLayout.fixed)
              .bordered(true)
              .pagination(antdBooleans.`false`)
              .dataSourceVarargs(rowCreator.resultRows: _*)
              .columnsVarargs(
                statusColumn,
                testInputColumns(inputKeys),
                dmnRowColumn(inputKeys, outputKeys),
                inOutColumns("Matched Input", inputKeys, _.inputs),
                inOutColumns("Output", outputKeys, _.outputs)
              )
          )
        )
  }

  private val statusColumn = {
    ColumnType[TableRow]
      .setTitle("")
      .setDataIndex("icon")
      .setKey("icon")
      .setRender((_, row, _) =>
        RenderedCell[TableRow]()
          .setChildren(icon(row.status))
          .setProps(CellType().setRowSpan(row.totalRowSpan))
      )
  }

  private def testInputColumns(testInputKeys: Seq[String]) =
    ColumnGroupType[TableRow](js.Array())
      .setTitleReactElement("Test Input(s)")
      .setChildrenVarargs(
        testInputKeys.map(in =>
          ColumnType[TableRow]
            .setTitle(in)
            .setDataIndex(in)
            .setEllipsis(true)
            .setKey(in)
            .setRender((_, row, _) =>
              renderTextCell(row.testInputs(in))
                .setProps(CellType().setRowSpan( row.totalRowSpan))
            )
        ): _*
      )

  private def dmnRowColumn(inputKeys: Seq[String], outputKeys: Seq[String]) = {
    ColumnType[TableRow]
      .setTitle("Dmn Row")
      .setEllipsis(true)
      .setDataIndex("DmnRow")
      .setKey("DmnRow")
      .setRender { (_, row, _) =>
        row.outputMessage match {
          case Some(msg) =>
            val colSpan =
              inputKeys.length + outputKeys.length + 1
            renderTextCell(msg)
              .setProps(
                CellType()
                  .setColSpan(colSpan)
                  .setClassName(s"${row.status}-cell")
              )
          case _ => renderTextCell(row.dmnRowIndex.toString)
        }
      }
  }

  private def inOutColumns(
      title: String,
      keys: Seq[String],
      inOutMap: TableRow => Map[String, String]
  ) = {
    ColumnGroupType[TableRow](js.Array())
      .setTitleReactElement(s"$title(s)")
      .setChildrenVarargs(
        keys.map { key =>
          ColumnType[TableRow]
            .setTitle(key)
            .setEllipsis(true)
            .setDataIndex(key)
            .setKey(key)
            .setRender((_, row, _) => {
              val value =
                if (inOutMap(row).isEmpty)
                  row.outputMessage.getOrElse("-")
                else inOutMap(row)(key)
              val colSpan =
                row.outputMessage.map(_ => 0).getOrElse(1)
              renderTextCell(value)
                .setProps(
                  CellType()
                    .setColSpan(colSpan)
                )
            })
        }: _*
      )
  }

  private def renderTextCell(text: String) = {
    RenderedCell[TableRow]()
      .setChildren(
        textWithTooltip(text, text)
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
    val key: String,
    val status: EvalStatus,
    val testInputs: Map[String, String],
    var inputRowSpan: Int,
    val dmnRowIndex: Int,
    val inputs: Map[String, String],
    val outputs: Map[String, String],
    val outputMessage: Option[String],
    val children: js.Array[TableRow]
) extends js.Object {

  def totalRowSpan: Double = children.size + inputRowSpan


  def toChildRow(): TableRow = {
    this.inputRowSpan = 0
    this
  }
}

case class RowCreator(
    dmnEvalResult: DmnEvalResult
) {
  val DmnEvalResult(
    dmn,
    inputKeys,
    outputKeys,
    evalResults,
    missingRules
  ) = dmnEvalResult

  lazy val resultRows: Seq[TableRow] =
    evalResults.sortBy(_.decisionId).flatMap {
      case DmnEvalRowResult(status, _, testInputs, Nil, maybeError) =>
        Seq(
          new TableRow(
            testInputs.values.mkString("-"),
            status,
            testInputs,
            1,
            0,
            Map.empty,
            Map.empty,
            Some(maybeError.map(_.msg).getOrElse("NOT FOUND")),
            js.Array()
          )
        )
      case DmnEvalRowResult(
            status,
            _,
            testInputs,
            matchedRules,
            maybeError
          ) =>
        val rows =
          matchedRules.zipWithIndex.map {
            case (MatchedRule(ruleId, inputs, outputMap), index) =>
              new TableRow(
                testInputs.values.mkString("-") + index,
                status,
                testInputs,
                1,/* if (index == 0)
                  matchedRules.size +
                    maybeError.map(_ => 1).getOrElse(0) // add an extra row
                else 0,*/
                rowIndex(ruleId),
                inputKeys.zip(inputs).toMap,
                outputMap,
                None,
                js.Array()
              )
          }
        maybeError
          .map(msg =>
            Seq(new TableRow(
              testInputs.values.mkString("-") + "_error",
              status,
              testInputs,
              1,
              1,
              Map.empty,
              Map.empty,
              Some(msg.msg),
             js.Array(rows.map(_.toChildRow()): _*)
            ))
          ).getOrElse(rows)

        //val outputs = outputMap(matchedRules)


    } ++ missingRules.map{
      case DmnRule(index, ruleId, inputs, outputs) =>
        new TableRow(
          ruleId + index + "Warn",
          EvalStatus.WARN,
          inputKeys.map(_ -> "").toMap,
          1,
          index,
          Map.empty,
          Map.empty,
          Some("There are no Test Inputs that match this Rule."),
          js.Array(new TableRow(
            ruleId + index,
            EvalStatus.WARN,
            inputKeys.map(_ -> "").toMap,
            0,
            index,
            inputKeys.zip(inputs).toMap,
            outputKeys.zip(outputs).toMap,
            None,
            js.Array()
          ))
        )
    }

  private def rowIndex(ruleId: String) =
    dmn.rules.find(_.ruleId == ruleId).map(_.index).getOrElse(-1)

}
