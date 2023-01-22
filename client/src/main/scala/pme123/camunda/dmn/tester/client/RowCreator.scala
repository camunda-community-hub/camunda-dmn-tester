package pme123.camunda.dmn.tester.client

import pme123.camunda.dmn.tester.shared.*
import scala.scalajs.js
import be.doeraene.webcomponents.ui5.*
import be.doeraene.webcomponents.ui5.configkeys.*
import com.raquo.laminar.api.L.{*, given}

import io.circe.*
import io.circe.syntax.*
import io.circe.generic.auto.*

case class RowCreator(
    dmnEvalResult: DmnEvalResult
):
  val DmnEvalResult(
    dmn,
    inputKeys,
    outputKeys,
    evalResults,
    missingRules
  ) = dmnEvalResult

  println(s"DMN: ${dmn.asJson}")
  private lazy val evaluatedRows: Seq[TableRow] =
    evalResults.sortBy(_.decisionId).flatMap {
      case DmnEvalRowResult(status, _, testInputs, Nil, maybeError) =>
        Seq(
          new TableRow(
            testInputs.values.mkString("-"),
            status,
            testInputs,
            1,
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
          matchedRules.zipWithIndex.map {
            case (MatchedRule(ruleId, rowIndex, inputs, outputs), index) =>
              new TableRow(
                testInputs.values.mkString("-") + index,
                status,
                testInputs,
                1,
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
                1,
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
        1,
        NotTested(index.toString),
        Seq.empty,
        outputKeys.zip(outputs.map(NotTested.apply)),
        Some("There are no Test Inputs that match this Rule."),
        Seq(
          new TableRow(
            ruleId + index,
            EvalStatus.WARN,
            inputKeys.map(_ -> "").toMap,
            0,
            NotTested(index.toString),
           rowInputs(ruleId, matchedInputKeys(evalResults).zip(inputs)),
            outputKeys.zip(outputs.map(NotTested.apply)),
            None,
            Seq()
          )
        )
      )
    }

  lazy val successful =
    val filteredRows = resultRows
      .filter(_.status == EvalStatus.INFO)
    if (filteredRows.isEmpty)
      Seq(h4("No successful tests"))
    else
      Seq(
        h3("Successful Tests ", icon(EvalStatus.INFO)),
        Table(
          className := "testResultsTable",
          _.mode := TableMode.MultiSelect,
          _.stickyColumnHeader := true,
          _.slots.columns := inputKeysColumns(false),
          _.slots.columns := Table.column(
            span("Dmn Row")
          ),
          _.slots.columns := inputKeysColumns(true),
          _.slots.columns := outputKeysColumns,
          filteredRows
            .map(r =>
              Table.row { tr =>
                //  Seq(tr.cell(icon(r.status))) ++
                ((inputKeys.map(r.testInputs(_).toString).toSeq :+
                  r.dmnRowIndex.intValue.toString) ++
                  r.inputs.map(_._2) ++
                  r.outputs.map(_._2.value))
                  .map(ellipsis(_, tr))

              }
            )
        )
      )

  lazy val noMatchingRows =
    val filteredRows = resultRows
      .filter(r =>
        r.status == EvalStatus.WARN && r.outputMessage == Some(
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

  lazy val noMatchingInputs =
    println(s"outputKeys: $outputKeys")
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
          _.slots.columns := outputKeys.map(ik =>
            Table.column(
              className := "resultOutputHeader",
              title := "Returned Output",
              span(ik)
            )
          ),
          filteredRows
            .map(r =>
              Table.row { tr =>
                (Seq(r.dmnRowIndex.intValue.toString) ++
                  r.outputs.map(_._2.value))
                  .map(ellipsis(_, tr))
              }
            )
        )
      )

  lazy val errorRows =
    val filteredRows = resultRows
      .filter(r => r.status == EvalStatus.ERROR)
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

  lazy val resultRows: Seq[TableRow] = ({
    evaluatedRows.groupBy(_.testInputs.values.toSeq).map {
      case (_, rows) if rows.size > 1 & rows.head.children.length == 0 =>
        rows.head.toParentRow(rows.tail.map(_.toChildRow()))
      case (_, others) => others.head
    }
  }.toSeq ++ missingRows).sortBy(_.dmnRowIndex.intValue).sortBy(_.status)

  val manyCols = (inputKeys.size * 2 + outputKeys.size) > 4
  def ellipsis(value: String, tableRow: TableRow.type) =
    val maxSize = 14
    tableRow.cell(
      title := value,
      if (manyCols && value.size > maxSize) value.take(maxSize) + ".."
      else value
    )

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
      title := "Result Output",
    )
  )

  private def rowIndex(ruleId: String) =
    dmn.rules.find(_.ruleId == ruleId).map(_.index).getOrElse(-1)

  private def rowInputs(ruleId: String, inputs: Seq[(String, String)]) =
    if(dmn.dmnConfig.testUnit)
        val rInputs = dmn.rules.find(_.ruleId == ruleId).map(_.inputs).getOrElse(Seq.empty)
        inputs.zipWithIndex.map{case ((k -> v), index) => k -> rInputs(index)}
    else
      inputs
  
  private def matchedRowsTable(row: TableRow) =
    if (row.children.isEmpty)
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
              //  Seq(tr.cell(icon(r.status))) ++
              (Seq(r.dmnRowIndex.intValue.toString) ++
                r.inputs.map(_._2) ++
                r.outputs.map(_._2.value))
                .map(ellipsis(_, tr))
            }
          )
      )

end RowCreator
