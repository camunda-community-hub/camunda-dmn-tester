package pme123.camunda.dmn.tester.client.runner

import pme123.camunda.dmn.tester.client.{textWithTooltip, withTooltip}
import pme123.camunda.dmn.tester.shared.EvalStatus.ERROR
import pme123.camunda.dmn.tester.shared.HandledTesterException.EvalException
import pme123.camunda.dmn.tester.shared.{DmnEvalRowResult, _}
import slinky.core.FunctionalComponent
import slinky.core.WithAttrs.build
import slinky.core.annotations.react
import slinky.core.facade.Hooks.useState
import slinky.core.facade.ReactElement
import slinky.web.html._
import typings.antDesignIcons.components.AntdIcon
import typings.antDesignIconsSvg.mod
import typings.antd.antdStrings.primary
import typings.antd.components._
import typings.antd.listMod.{ListLocale, ListProps}
import typings.antd.tableInterfaceMod.{ColumnGroupType, ColumnType, TableRowSelection}
import typings.antd.{antdBooleans, antdStrings => aStr}
import typings.rcTable.interfaceMod.{CellType, RenderedCell, TableLayout}
import typings.react.mod.CSSProperties

import scala.scalajs.js
import scala.scalajs.js.|

@react object EvalResultsCard {

  case class Props(
      evalResults: Seq[Either[EvalException, DmnEvalResult]],
      isLoaded: Boolean,
      maybeError: Option[String],
      onCreateTestCases: DmnConfig => Unit
  )

  val component: FunctionalComponent[Props] = FunctionalComponent[Props] {
    props =>
      val Props(evalResults, isLoaded, maybeError, onCreateTestCases) = props
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
              EvalResultsList(evalResults, onCreateTestCases)
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
      evalResults: Seq[Either[EvalException, DmnEvalResult]],
      onCreateTestCases: DmnConfig => Unit
  )

  val component: FunctionalComponent[Props] = FunctionalComponent[Props] {
    props =>
      val Props(evalResults, onCreateTestCases) = props
      // sort first exceptions - then decisionId
      val sortedResults = evalResults.sortWith {
        case (_: Right[_, _], _: Left[_, _]) => false
        case (_: Left[_, _], _: Right[_, _]) => true
        case (Right(a), Right(b)) =>
          a.dmn.id < b.dmn.id
        case (Left(a), Left(b)) =>
          a.decisionId < b.decisionId
      }
      List
        .withProps(
          ListProps()
            .setDataSource(js.Array(sortedResults: _*))
            .setLocale(
              ListLocale().setEmptyText(
                Empty().description("There are no Tests selected:(").build
              )
            )
            .setRenderItem(
              (evalResult: Either[EvalException, DmnEvalResult], _) =>
                EvalResultsItem(evalResult, onCreateTestCases)
            )
        )
  }
}

@react object EvalResultsItem {

  val outputTitle = "Output"

  case class Props(
      evalResult: Either[EvalException, DmnEvalResult],
      onCreateTestCases: DmnConfig => Unit
  )

  val component: FunctionalComponent[Props] = FunctionalComponent[Props] {
    case Props(Left(EvalException(decisionId, msg)), _) =>
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
          ),
          onCreateTestCases
        ) =>
      val rowCreator = RowCreator(er)
      val (selectedRows, setSelectedRows) = useState(Seq.empty[TableRow])

      def evalTable = {
        Table[TableRow]
          .withKey(dmn.id + "Key")
          .defaultExpandAllRows(true)
          .expandIcon(_ => "")
          .rowSelection(
            TableRowSelection[TableRow]()
              //.setHideSelectAll(true)
              .setRenderCell((_, row, _, ele) => {
                if (row.status == EvalStatus.INFO)
                  ele
                else
                  build(span(""))
              })
              .setCheckStrictly(false)
              .setOnSelectAll((selected, _, rows) =>
                if (selected)
                  setSelectedRows(rows.toSeq.filter(_.status == EvalStatus.INFO))
                else
                  setSelectedRows(Seq.empty)
              )
              .setOnSelect((row, selected, _, _) =>
                if (selected)
                  setSelectedRows(selectedRows :+ row)
                else
                  setSelectedRows(selectedRows.filterNot(_.key == row.key))
              )
          )
          .tableLayout(TableLayout.fixed)
          .bordered(true)
          .pagination(antdBooleans.`false`)
          .dataSourceVarargs(rowCreator.resultRows: _*)
          .columnsVarargs(
            statusColumn,
            testInputColumns(inputKeys),
            dmnRowColumn(inputKeys, outputKeys),
            inOutColumns("Matched Input", inputKeys, _.inputs),
            inOutColumns(outputTitle, outputKeys, _.outputs)
          )
      }

      List.Item
        .withKey(dmn.id)
        .className("list-item")(
          section(
            h2(Space(icon(er.maxEvalStatus), span(dmn.id))),
            p(s"Hitpolicy: ${dmn.hitPolicy}"),
            p("DMN: " + dmn.dmnConfig.dmnPath.mkString("/")),
            evalTable,
            withTooltip(
              "BE AWARE that this overwrites all existing Test Cases!",
              Button
                .`type`(primary)
                .block(true)
                .onClick { _ =>
                  val exConfig = dmn.dmnConfig
                  val newConfig = exConfig.copy(data =
                    exConfig.data.copy(testCases = selectedRows.map { row =>
                      TestCase(TesterValue.valueMap(row.testInputs), row.dmnRowIndex, TesterValue.valueMap(row.outputs))
                    }.toList)
                  )
                  onCreateTestCases(newConfig)
                }(
                  "Create Test Cases from the checked Rows"
                )
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
                .setProps(CellType().setRowSpan(row.totalRowSpan))
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
        row.renderRowIndexCol(inputKeys, outputKeys)
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
            .setRender((_, row, _) =>
              if (title == outputTitle)
                row.renderOutCell(key, inOutMap(row))
              else
                row.renderInCell(key, inOutMap(row))
            )
        }: _*
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
    var children: js.Array[TableRow],
    var maybeTestCase: Option[TestCase] = None
) extends js.Object {

  def totalRowSpan: Double = children.size + inputRowSpan

  def toParentRow(children: Seq[TableRow]): TableRow = {
    this.children = js.Array(children: _*)
    this
  }
  def toChildRow(): TableRow = {
    this.inputRowSpan = 0
    this
  }

  def renderRowIndexCol(
      inputKeys: Seq[String],
      outputKeys: Seq[String]
  ): ReactElement | RenderedCell[TableRow] =
    outputMessage match {
      case Some(msg) =>
        val colSpan =
          inputKeys.length + outputKeys.length + 1
        RenderedCell[TableRow]()
          .setChildren(
            textWithTooltip(msg, msg)
          )
          .setProps(
            CellType()
              .setColSpan(colSpan)
              .setClassName(s"$status-cell")
          )
      case _ =>
        testedCell(
          1,
          dmnRowIndex.toString,
          tc => tc.rowIndex.toString
        )

    }

  def renderInCell(
      key: String,
      inOutMap: Map[String, String]
  ): RenderedCell[TableRow] = {
    val (value, colSpan) = inOutValueColSpan(key, inOutMap)
    renderTextCell(value)
      .setProps(
        CellType()
          .setColSpan(colSpan)
      )
  }

  def renderOutCell(
      key: String,
      inOutMap: Map[String, String]
  ): RenderedCell[TableRow] = {
    val (value, colSpan) = inOutValueColSpan(key, inOutMap)
    testedCell(
      colSpan,
      value,
      tc =>
        tc.outputs
          .get(key)
          .map(_.valueStr)
          .getOrElse(s"There is no Output Key '$key''")
    )
  }

  private def inOutValueColSpan(
      key: String,
      inOutMap: Map[String, String]
  ): (String, Int) =
    (if (inOutMap.isEmpty)
       outputMessage.getOrElse("-")
     else inOutMap(key)) ->
      outputMessage.map(_ => 0).getOrElse(1)

  private def testedCell(
      colSpan: Int,
      actualVal: String,
      expVal: TestCase => String
  ) = {
    val (cssClass, tooltip) = maybeTestCase
      .map { testCase =>
        if (actualVal == expVal(testCase))
          "SUCCESS-cell" -> actualVal
        else
          "ERROR-cell" -> s"$actualVal did not match the expected one: ${expVal(testCase)}"

      }
      .getOrElse(s"INFO-cell" -> actualVal)

    RenderedCell[TableRow]()
      .setChildren(
        textWithTooltip(actualVal.toString, tooltip.toString)
      )
      .setProps(
        CellType()
          .setColSpan(colSpan)
          .setClassName(cssClass)
      )
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

  private lazy val evaluatedRows: Seq[TableRow] =
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
                1,
                /* if (index == 0)
                  matchedRules.size +
                    maybeError.map(_ => 1).getOrElse(0) // add an extra row
                else 0,*/
                rowIndex(ruleId),
                inputKeys.zip(inputs).toMap,
                outputMap,
                None,
                js.Array(),
                dmn.dmnConfig.findTestCase(testInputs)
              )
          }
        maybeError
          .map(msg =>
            Seq(
              new TableRow(
                testInputs.values.mkString("-") + "_error",
                status,
                testInputs,
                1,
                1,
                Map.empty,
                Map.empty,
                Some(msg.msg),
                js.Array(rows.map(_.toChildRow()): _*)
              )
            )
          )
          .getOrElse(rows)

    }
  private lazy val missingRows =
    missingRules.map { case DmnRule(index, ruleId, inputs, outputs) =>
      new TableRow(
        ruleId + index + "Warn",
        EvalStatus.WARN,
        inputKeys.map(_ -> "").toMap,
        1,
        index,
        Map.empty,
        Map.empty,
        Some("There are no Test Inputs that match this Rule."),
        js.Array(
          new TableRow(
            ruleId + index,
            EvalStatus.WARN,
            inputKeys.map(_ -> "").toMap,
            0,
            index,
            inputKeys.zip(inputs).toMap,
            outputKeys.zip(outputs).toMap,
            None,
            js.Array()
          )
        )
      )
    }

  lazy val resultRows: Seq[TableRow] = ({
    evaluatedRows.groupBy(_.testInputs.values.toSeq).map {
      case (_, rows) if rows.size > 1 & rows.head.children.length == 0 =>
        rows.head.toParentRow(rows.tail.map(_.toChildRow()))
      case (_, others) => others.head
    }
  }.toSeq ++ missingRows).sortBy(_.dmnRowIndex).sortBy(_.status)

  private def rowIndex(ruleId: String) =
    dmn.rules.find(_.ruleId == ruleId).map(_.index).getOrElse(-1)

}
