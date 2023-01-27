package pme123.camunda.dmn.tester.client

import be.doeraene.webcomponents.ui5
import be.doeraene.webcomponents.ui5.*
import be.doeraene.webcomponents.ui5.configkeys.*
import com.raquo.domtypes.generic.codecs.BooleanAsAttrPresenceCodec
import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveElement
import io.circe.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.scalajs.dom.HTMLElement
import pme123.camunda.dmn.tester.shared
import pme123.camunda.dmn.tester.shared.*

import scala.collection.View.Empty
import scala.scalajs.js

case class RowCreator(
    dmnEvalResult: DmnEvalResult
):

  lazy val testCasesTable: Seq[HtmlElement] =
    val filteredRows = resultRows
      .filter(r =>
        r.status == EvalStatus.INFO || (r.status == EvalStatus.ERROR && r.outputMessage.isEmpty)
      )
    allRowsVar.set(filteredRows.map(r => r.key -> r).toMap)
    if (filteredRows.isEmpty) Seq(h4("No successful tests"))
    else {
      val maxStatus = maxEvalStatus(filteredRows)
      val rows: Seq[HtmlElement] = filteredRows
        .map(r =>
          Table.row(
            accessKey := r.key,
            tr =>
              (inputKeys
                .map(r.testInputs(_).toString)
                .map(ellipsis(_, tr)) :+
                collectCellTable(r.children.map {
                  _.dmnRowIndex
                })) ++
                r.inputs.zipWithIndex.map { case _ -> index =>
                  collectCellTable(
                    r.children.map {
                      _.inputs(index)._2
                    }
                  )
                } ++
                r.outputs.zipWithIndex.map { case _ -> index =>
                  collectCellTable(
                    r.children.map {
                      _.outputs(index)._2
                    }
                  )
                }
          )
        )
      Seq(
        h3(
          if (maxStatus == EvalStatus.INFO)
            "Successful Tests "
          else "Failed TestCases",
          icon(maxStatus)
        ),
        if (maxStatus == EvalStatus.INFO) span("")
        else p("Your DMN is correct, but you expected some different results."),
        Table(
          className := "testResultsTable",
          _.mode := TableMode.MultiSelect,
          _.stickyColumnHeader := true,
          _.events.onSelectionChange.map(
            _.detail.selectedRows
              .map(r => allRowsVar.now()(r.accessKey))
              .toList
          ) --> selectedTableRowsVar,
          _.slots.columns := inputKeysColumns(false),
          _.slots.columns := Table.column(
            span("Dmn Row")
          ),
          _.slots.columns := inputKeysColumns(true),
          _.slots.columns := outputKeysColumns,
          rows
        )
      )
    }

  lazy val noMatchingRows: Seq[HtmlElement] =
    val filteredRows = resultRows
      .filter(r =>
        r.status == EvalStatus.WARN && r.outputMessage.contains(
          noMatchingRowsMsg
        )
      )
    if (filteredRows.isEmpty)
      Seq()
    else
      Seq(
        h3(
          "Test Inputs with no matching Row ",
          icon(EvalStatus.WARN)
        ),
        p("For the following inputs, there is no matching row in the DMN."),
        Table(
          className := "testResultsTable",
          _.stickyColumnHeader := true,
          _.slots.columns := inputKeysColumns(false),
          filteredRows
            .map(r =>
              Table.row { tr =>
                r.testInputs
                  .map(_._2.toString)
                  .map(ellipsis(_, tr))
                  .toSeq
              }
            )
        )
      )

  lazy val noMatchingInputs: Seq[HtmlElement] =
    val filteredRows = missingRows
    if (filteredRows.isEmpty)
      Seq()
    else
      Seq(
        h3("Rules with no matching Test Inputs ", icon(EvalStatus.WARN)),
        p(
          "There are no Test Inputs that match these Rules."
        ),
        Table(
          className := "testResultsTable",
          _.stickyColumnHeader := true,
          _.slots.columns := Table.column(
            span("Dmn Row")
          ),
          _.slots.columns := inputKeysColumns(false),
          _.slots.columns := outputKeysColumns,
          filteredRows
            .map(r =>
              Table.row { tr =>
                (Seq(r.dmnRowIndex.intValue.toString) ++
                  r.inputs.map(_._2) ++
                  r.outputs.map(_._2.value))
                  .map(ellipsis(_, tr))
              }
            )
        )
      )

  lazy val errorRows: Seq[HtmlElement] =
    val filteredRows = resultRows
      .filter(r => r.status == EvalStatus.ERROR && r.outputMessage.nonEmpty)
    if (filteredRows.isEmpty)
      Seq()
    else
      Seq(
        h3(
          "Tests with Errors ",
          icon(EvalStatus.ERROR)
        ),
        p("For the following inputs, evaluating the DMN created an error."),
        Table(
          className := "testResultsTable",
          _.stickyColumnHeader := true,
          _.slots.columns := inputKeysColumns(false),
          _.slots.columns := Table.column("Error Message"),
          filteredRows
            .map(r =>
              Table.row { tr =>
                r.testInputs
                  .map(_._2.toString)
                  .map(value =>
                    tr.cell(
                      title := value,
                      value
                    )
                  )
                  .toSeq :+
                  tr.cell(
                    r.outputMessage.getOrElse("no error message"),
                    matchedRowsTable(r)
                  )
              }
            )
        )
      )

  lazy val failedTestCasePopup: HtmlElement =
    Popover(
      _.placementType := PopoverPlacementType.Bottom,
      _.showAtFromEvents(openPopoverBus.events.collect {
        case Some(opener) -> _ =>
          opener
      }),
      _.closeFromEvents(openPopoverBus.events.collect { case None -> _ => () }),
      p(child <-- openPopoverBus.events.collect { case _ -> msg =>
        msg
      })
    )

  private lazy val selectedTableRowsVar: Var[List[TableRow]] = Var(List.empty)
  private lazy val DmnEvalResult(
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
            NotTested("0"),
            Seq.empty,
            Seq.empty,
            Some(maybeError.map(_.msg).getOrElse(noMatchingRowsMsg)),
            Seq()
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
          matchedRules.map {
            case MatchedRule(ruleId, rowIndex, inputs, outputs) =>
              new TableRow(
                testInputs.values.mkString("-"),
                status,
                testInputs,
                rowIndex,
                rowInputs(ruleId, inputs),
                outputs,
                None,
                Seq()
              )
          }
        maybeError
          .map(msg =>
            Seq(
              new TableRow(
                testInputs.values.mkString("-") + "_error",
                status,
                testInputs,
                NotTested("1"),
                Seq.empty,
                Seq.empty,
                Some(msg.msg),
                Seq(rows.map(_.toChildRow()): _*)
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
        NotTested(index.toString),
        inputKeys.zip(inputs),
        // if there are errors in the evaluation - there might be no outputs.
        outputKeys.zip(outputs.map(NotTested.apply)),
        outputMessage = None,
        children = Seq.empty
      )
    }

  private lazy val allRowsVar = Var(Map.empty[String, TableRow])

  private lazy val resultRows: Seq[TableRow] = {
    evaluatedRows.groupBy(_.testInputs.values.toSeq).map {
      // work with children - to support also HitPolicies with multiple Result Rules
      case (_, rows) if rows.head.children.isEmpty =>
        rows.head.toParentRow(rows.map(_.toChildRow()))
      // errors with list of rows that are involved (HitPolicy UNIQUE with multiple results)
      case (_, others) => others.head
    }
  }.toSeq.sortBy(_.dmnRowIndex.intValue).sortBy(_.status)

  private def ellipsis(
      value: String,
      tableRow: TableRow.type,
      clsName: String = "notTestedCell",
      msg: Option[String] = None
  ): HtmlElement =
    ellipsis(value, tableRow.cell(), clsName, msg)

  private def ellipsis(
      value: String,
      elem: HtmlElement,
      clsName: String,
      msg: Option[String]
  ): HtmlElement =
    val manyCols = (inputKeys.size * 2 + outputKeys.size) > 4
    val maxSize = 14
    val shorten = manyCols && value.length > maxSize
    elem.amend(
      title := (if (shorten) value else ""),
      className := clsName,
      if (shorten) value.take(maxSize) + ".." else value,
      onMouseOver --> (e => e.target.asInstanceOf[HTMLElement].focus()),
      onMouseOver
        .filter(_ => msg.nonEmpty)
        .map(_.target.asInstanceOf[HTMLElement])
        .map(Some(_) -> msg.get) --> openPopoverBus,
      onMouseOut.mapTo(None -> "") --> openPopoverBus
    )

  private def cellParagr = p(className := "cellParagr")
  private def ellipsis(
      result: TestedValue
  ): HtmlElement = result match
    case TestFailure(value, msg) =>
      ellipsis(value, cellParagr, "failedCell", Some(msg))
    case TestSuccess(value) =>
      ellipsis(value, cellParagr, "succeededCell", None)
    case NotTested(value) =>
      ellipsis(value, cellParagr, "notTestedCell", None)

  private def ellipsis(
      value: String
  ): HtmlElement =
    ellipsis(value, p(), "notTestedCell", None)

  private def inputKeysColumns(
      matchedInputKeys: Boolean
  ) = inputKeys.map(ik =>
    Table.column(
      className := (if (matchedInputKeys) "matchedInputHeader"
                    else "resultInputHeader"),
      span(ik),
      title := (if (matchedInputKeys) "Matched Input" else "Test Input")
    )
  )

  private def outputKeysColumns = outputKeys.map(ik =>
    Table.column(
      className := "resultOutputHeader",
      span(ik),
      title := "Result Output"
    )
  )

  private def rowInputs(ruleId: String, inputs: Seq[(String, String)]) =
    if (dmn.dmnConfig.testUnit)
      val rInputs =
        dmn.rules.find(_.ruleId == ruleId).map(_.inputs).getOrElse(Seq.empty)
      inputs.zipWithIndex.map { case (k -> _, index) => k -> rInputs(index) }
    else inputs

  private def matchedRowsTable(row: TableRow) =
    if (row.children.isEmpty || row.children.head.outputs.isEmpty)
      span("")
    else
      Table(
        className := "testResultsTable",
        _.slots.columns := Table.column(
          className := "smallColHeader",
          span("Dmn Row")
        ),
        _.slots.columns := inputKeysColumns(true),
        _.slots.columns := outputKeysColumns,
        row.children
          .map(r =>
            Table.row { tr =>
              (Seq(r.dmnRowIndex.intValue.toString) ++
                r.inputs.map(_._2) ++
                r.outputs.map(_._2.value))
                .map(ellipsis(_, tr))
            }
          )
      )

  // if HitPolicy COLLECT one inputs variation may have more than one result
  private def collectCellTable(
      values: Seq[TestedValue | String]
  ) =
    val cells =
      values
        .map {
          case v: TestedValue =>
            ellipsis(v)
          case v: String =>
            ellipsis(v)
        }
    TableRow.cell(
      cells
    )

  private lazy val exConfig = dmn.dmnConfig
  lazy val newConfigSignal: Signal[DmnConfig] =
    selectedTableRowsVar.signal.map(selectedRows =>
      exConfig.copy(data = exConfig.data.copy(testCases = selectedRows.map {
        row =>
          val results = row.children
            .map(r =>
              TestResult(
                r.dmnRowIndex.value.toInt,
                TesterValue.valueMap(asStrMap(r.outputs).toMap)
              )
            )
            .toList
          TestCase(
            TesterValue.valueMap(asStrMap(row.testInputs)),
            results
          )
      }))
    )

  private lazy val openPopoverBus: EventBus[(Option[HTMLElement], String)] =
    new EventBus

end RowCreator
