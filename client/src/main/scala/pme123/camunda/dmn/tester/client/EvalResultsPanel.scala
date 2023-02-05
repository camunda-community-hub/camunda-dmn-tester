package pme123.camunda.dmn.tester.client

import be.doeraene.webcomponents.ui5.*
import be.doeraene.webcomponents.ui5.configkeys.*
import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom.HTMLElement
import pme123.camunda.dmn.tester.shared.*

case class EvalResultsPanel(
    result: DmnEvalResult,
    dmnConfigPathSignal: Signal[String],
    dmnConfigsVar: Var[Seq[DmnConfig]]
):
  private lazy val dmnConfig = result.dmnTables.dmnConfig
  private lazy val mainTable = result.dmnTables.mainTable
  private lazy val creator = RowCreator(result)
  private lazy val saveConfigBus = EventBus[Boolean]()

  private lazy val comp = Panel(
    _.accessibleRole := PanelAccessibleRole.Complementary,
    className := "testResultsPanel",
    className := "flex-column",
    className := "full-width",
    _.collapsed := true,
    _.slots.header := panelHeader(dmnConfig, result.maxEvalStatus),
    div(
      child <-- submitDmnConfig
    ),
    p(
      b(
        if (dmnConfig.testUnit) "Unit Test"
        else "Integrated Test - experimental ☠️ \uD83D\uDE0A"
      )
    ),
    p(s"Hit Policy: ${mainTable.hitPolicy}"),
    mainTable.aggregation.map(a => s"Aggregation: $a").getOrElse(""),
    if (dmnConfig.data.variables.nonEmpty)
      p(
        "Variables: " + dmnConfig.data.variables
          .map(_.key)
          .mkString(", ")
      )
    else "",
    creator.errorRows,
    creator.noMatchingRows,
    creator.noMatchingInputs,
    creator.testCasesTable,
    creator.creatorPopover,
    Button(
      _.icon := IconName.`add-activity-2`,
      "Create Test Cases from the checked Rows",
      width := "100%",
      _.events.onClick.mapTo(true) --> saveConfigBus,
      onMouseOver --> (e => e.target.asInstanceOf[HTMLElement].focus()),
      onMouseOver
        .map(_.target.asInstanceOf[HTMLElement])
        .map(Some(_)) --> openPopoverBus,
      onMouseOut.mapTo(None) --> openPopoverBus
    ),
    warnCreateTestCasesPopup
  )

  private lazy val openPopoverBus: EventBus[Option[HTMLElement]] = new EventBus
  private lazy val warnCreateTestCasesPopup =
    generalPopover(
      p("BE AWARE that this overwrites all existing Test Cases!")
    )

  private lazy val submitDmnConfig = saveConfigBus.events
    .withCurrentValueOf(creator.newConfigSignal, dmnConfigPathSignal)
    .flatMap { case (_, newConfig: DmnConfig, path) =>
      if (newConfig.hasErrors)
        EventStream.fromValue(
          errorMessage(
            ErrorMessage(
              "Validation Error(s)",
              "There are incorrect data, please correct them in the Config Editor before creating Test Cases."
            )
          )
        )
      else
        BackendClient
          .updateConfig(newConfig, path)
          .map(responseToHtml(configs => {
            dmnConfigsVar.set(configs)
            span("")
          }))
    }
object EvalResultsPanel:
  def apply(
      result: DmnEvalResult,
      dmnConfigPathSignal: Signal[String],
      dmnConfigsVar: Var[Seq[DmnConfig]]
  ): HtmlElement = new EvalResultsPanel(
    result,
    dmnConfigPathSignal,
    dmnConfigsVar
  ).comp
end EvalResultsPanel
