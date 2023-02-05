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
import scala.collection.immutable
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
        .map {
          case rtr @ ResultTableRow(
                key,
                status,
                testInputs,
                matchedRulesPerTable,
                outputMessage
              ) =>
            val DmnTable(
              decisionId,
              name,
              hitPolicy,
              aggregation,
              inputCols,
              outputCols,
              ruleRows
            ) = allDmnTables.mainTable
            val matchedInputs: Seq[HtmlElement] = inputCols.zipWithIndex.map {
              case _ -> index =>
                collectCellTable(
                  rtr.mainMatchedRules.map(mRule => mRule.inputs(index)._2)
                )
            }
            val matchedOutputs: Seq[HtmlElement] = outputCols.zipWithIndex.map {
              case _ -> index =>
                collectCellTable(
                  rtr.mainMatchedRules.map(mRule => mRule.outputs(index)._2)
                )
            }

            Table.row(
              accessKey := rtr.key,
              tr =>
                (inputKeys
                  .map(rtr.testInputs(_).toString)
                  .map(ellipsis(_, tr)) :+
                  collectCellTable(rtr.mainMatchedRules.map(_.rowIndex))) ++
                  matchedInputs ++
                  matchedOutputs ++
                  (if (rtr.hasRequiredTables)
                     Seq(tr.cell(showRequiredTables(rtr)))
                   else Seq.empty)
            )
        }
      val matchedInputsCols: Seq[HtmlElement] =
        //  allDmnTables.tables.flatMap(t =>
        matchedInputsColumns(
          allDmnTables.mainTable.decisionId,
          filteredRows.head.mainInputKeys
        )
      //   )
      val matchedOutputsCols: Seq[HtmlElement] =
        //  if (inputKeys.size == filteredRows.head.inputs.size)
        resultOutputsColumns(
          allDmnTables.mainTable.decisionId,
          filteredRows.head.mainOutputKeys
        )
      /*   else
          Seq(
            Table.column(
              className := "matchedInputHeader",
              "No Matching Inputs available.",
              Icon(_.name := IconName.`question-mark`, marginLeft := "5px"),
              onMouseOver --> (e => e.target.asInstanceOf[HTMLElement].focus()),
              onMouseOver
                .map(_.target.asInstanceOf[HTMLElement])
                .map(
                  Some(
                    _
                  ) -> ("The reason is that your integrated Test, has inputs that are not specified in the Dmn Config. " +
                    "This is fine in Integrated Tests!")
                ) --> openPopoverBus,
              onMouseOut.mapTo(None -> "") --> openPopoverBus
            )
          )*/

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
          _.slots.columns := inputKeysColumns(),
          _.slots.columns := Table.column(
            span("Dmn Row")
          ),
          _.slots.columns := matchedInputsCols,
          _.slots.columns := matchedOutputsCols,
          if (allDmnTables.hasRequiredTables)
            _.slots.columns := Table.column(width := "0")
          else "",
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
    if (filteredRows.isEmpty) Seq()
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
          _.slots.columns := inputKeysColumns(),
          filteredRows
            .map(r =>
              Table.row { tr =>
                r.testInputs
                  .filter(i => inputKeys.contains(i._1))
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
          _.slots.columns :=
            matchedInputsColumns(
              allDmnTables.mainTable.decisionId,
              filteredRows.head.mainInputKeys
            ),
          _.slots.columns :=
            resultOutputsColumns(
              allDmnTables.mainTable.decisionId,
              filteredRows.head.mainOutputKeys
            ),
          filteredRows.map(r => simpleRows(r.mainMatchedRules))
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
          _.slots.columns := inputKeysColumns(),
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

  lazy val generalPopup: HtmlElement =
    Popover(
      _.placementType := PopoverPlacementType.Bottom,
      _.showAtFromEvents(openPopoverBus.events.collect {
        case Some(opener) -> _ =>
          opener
      }),
      _.closeFromEvents(openPopoverBus.events.collect { case None -> _ =>
        ()
      }),
      _.slots.header <-- openPopoverBus.events.collect {
        case _ -> (resultTableRow: ResultTableRow) =>
          h2(s"Required Tables for ${resultTableRow.mainDecisionId}")
        case _ => span("")
      },
      child <-- openPopoverBus.events.collect {
        case _ -> (msg: String) => div(msg)
        case _ -> (resultTableRow: ResultTableRow) =>
          displayRequiredTables(resultTableRow)
      }
    )

  private lazy val selectedTableRowsVar: Var[List[ResultTableRow]] = Var(
    List.empty
  )
  private lazy val DmnEvalResult(
    allDmnTables,
    inputKeys,
    outputKeys,
    evalResults,
    missingRules
  ) = dmnEvalResult

  private lazy val evaluatedRows: Seq[ResultTableRow] =
    evalResults.flatMap {
      case r @ DmnEvalRowResult(status, testInputs, _, maybeError)
          if r.hasNoMatch =>
        Seq(
          new ResultTableRow(
            testInputs.values.mkString("-"),
            status,
            testInputs,
            Seq.empty,
            Some(maybeError.map(_.msg).getOrElse(noMatchingRowsMsg))
          )
        )
      case DmnEvalRowResult(
            status,
            testInputs,
            matchedRulesPerTable,
            maybeError
          ) =>
        val mainRow = ResultTableRow(
          testInputs.values.mkString("-"),
          status,
          testInputs,
          matchedRulesPerTable,
          maybeError.map(_.msg)
        )
        Seq(
          mainRow
        )

    }
  private lazy val missingRows: Seq[ResultTableRow] =
    missingRules.map { case DmnRule(index, ruleId, inputs, outputs) =>
      ResultTableRow(
        ruleId + index + "Warn",
        EvalStatus.WARN,
        inputKeys.map(_ -> "").toMap,
        Seq(
          MatchedRulesPerTable(
            allDmnTables.dmnConfig.decisionId,
            Seq(
              MatchedRule(
                ruleId,
                NotTested(index.toString),
                inputs,
                outputs.map(o => o._1 -> NotTested(o._2))
              )
            ),
            None
          )
        ),
        outputMessage = None
      )
    }

  private lazy val allRowsVar = Var(Map.empty[String, ResultTableRow])

  private lazy val resultRows: Seq[ResultTableRow] =
    evaluatedRows
      .sortBy(_.mainIndex.intValue)
      .sortBy(_.status)

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
    val maxSize = 20
    val shorten = manyCols && value.length > maxSize
    val valueStr = if (value.isBlank) "-" else value
    elem.amend(
      className := clsName,
      if (shorten) value.take(maxSize) + ".." else valueStr,
      onMouseOver --> (e => e.target.asInstanceOf[HTMLElement].focus()),
      onMouseOver
        .filter(_ => msg.nonEmpty || shorten)
        .map(_.target.asInstanceOf[HTMLElement])
        .map(Some(_) -> msg.getOrElse(value)) --> openPopoverBus,
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
      inputKs: Seq[String] = inputKeys
  ): Seq[HtmlElement] =
    inputKs.map(ik =>
      Table.column(
        className := "resultInputHeader",
        span(ik),
        title := "Test Input"
      )
    )

  private def matchedInputsColumns(
      decisionId: String,
      inputs: Seq[String]
  ) =
    inputs.map(ik =>
      Table.column(
        className := "matchedInputHeader",
        span(ik),
        onMouseOver --> (e => e.target.asInstanceOf[HTMLElement].focus()),
        onMouseOver
          .map(_.target.asInstanceOf[HTMLElement])
          .map(
            Some(
              _
            ) -> s"Matched Input from the Decision Table $decisionId"
          ) --> openPopoverBus,
        onMouseOut.mapTo(None -> "") --> openPopoverBus
      )
    )

  private def resultOutputsColumns(
      decisionId: String,
      outputKeys: Seq[String]
  ) =
    outputKeys.map(key =>
      Table.column(
        className := "resultOutputHeader",
        span(key),
        onMouseOver --> (e => e.target.asInstanceOf[HTMLElement].focus()),
        onMouseOver
          .map(_.target.asInstanceOf[HTMLElement])
          .map(
            Some(
              _
            ) -> s"Result Output from the Decision Table $decisionId"
          ) --> openPopoverBus,
        onMouseOut.mapTo(None -> "") --> openPopoverBus
      )
    )

  private def matchedRowsTable(row: ResultTableRow) =
    if (row.tableWithMatchedRules.isEmpty) span("")
    else
      val maybeTable = row.tableWithMatchedRules
      maybeTable
        .map(table =>
          Table(
            className := "testResultsTable",
            _.slots.columns := Table.column(
              className := "smallColHeader",
              span("Dmn Row")
            ),
            _.slots.columns :=
              matchedInputsColumns(
                table.decisionId,
                table.inputKeys
              ),
            _.slots.columns :=
              resultOutputsColumns(
                table.decisionId,
                table.outputKeys
              ),
            simpleRows(table.matchedRules)
          )
        )
        .getOrElse(span("No matched Rules."))

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

  private lazy val exConfig = allDmnTables.dmnConfig
  lazy val newConfigSignal: Signal[DmnConfig] =
    selectedTableRowsVar.signal.map(selectedRows =>
      exConfig.copy(data = exConfig.data.copy(testCases = selectedRows.map {
        row =>
          val results = row.mainMatchedRules
            .map(r =>
              TestResult(
                r.rowIndex.intValue,
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

  private lazy val openPopoverBus: EventBus[(Option[HTMLElement], Any)] =
    new EventBus

  private def showRequiredTables(tableRow: ResultTableRow) =
    Button(
      _.icon := IconName.`table-view`,
      onMouseOver --> (e => e.target.asInstanceOf[HTMLElement].focus()),
      onMouseOver
        .map(_.target.asInstanceOf[HTMLElement])
        .map(Some(_) -> tableRow) --> openPopoverBus,
      onMouseOut.mapTo(None -> "") --> openPopoverBus
    )
  private def displayRequiredTables(
      tableRow: ResultTableRow
  ) =
    div(
      allDmnTables.requiredTables.map {
        case DmnTable(
              decisionId,
              _,
              hitPolicy,
              aggregation,
              inputCols,
              outputCols,
              _
            ) =>
          Panel(
            _.accessibleRole := PanelAccessibleRole.Complementary,
            className := "testResultsPanel",
            className := "flex-column",
            className := "full-width",
            h2(s"Table: $decisionId"),
            p(s"Hitpolicy: ${hitPolicy}"),
            aggregation.map(a => s"Aggregation: $a").getOrElse(""),
            tableRow.matchedRulesPerTable
              .find(_.decisionId == decisionId)
              .map { mrpt =>
                val rows =
                  mrpt.matchedRules.map(mRules =>
                    Table.row(tr =>
                      ((mRules.rowIndex.value +:
                        mRules.inputs.map(_._2)) ++
                        mRules.outputs.map(_._2.value))
                        .map(ellipsis(_, tr))
                    )
                  )

                Table(
                  className := "testResultsTable",
                  _.stickyColumnHeader := true,
                  _.slots.columns := Table.column(
                    span("Dmn Row")
                  ),
                  _.slots.columns := matchedInputsColumns(
                    decisionId,
                    mrpt.inputKeys
                  ),
                  _.slots.columns := resultOutputsColumns(
                    decisionId,
                    mrpt.outputKeys
                  ),
                  rows
                )

              }
              .getOrElse(span(s"No matching table found for $decisionId"))
          )
      }
    )

  private def simpleRows(matchedRules: Seq[MatchedRule]) =
    matchedRules.map(mRules =>
      Table.row(tr =>
        ((mRules.rowIndex.value +:
          mRules.inputs.map(_._2)) ++
          mRules.outputs.map(_._2.value))
          .map(ellipsis(_, tr))
      )
    )

end RowCreator
