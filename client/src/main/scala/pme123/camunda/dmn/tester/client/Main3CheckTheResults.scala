package pme123.camunda.dmn.tester.client

import be.doeraene.webcomponents.ui5.*
import be.doeraene.webcomponents.ui5.configkeys.*
import com.raquo.laminar.api.L.{*, given}
import com.raquo.airstream.core.Signal
import org.scalajs.dom.html
import com.raquo.laminar.nodes.ReactiveHtmlElement
import pme123.camunda.dmn.tester.shared.DmnConfig
import pme123.camunda.dmn.tester.shared.HandledTesterException.EvalException
import pme123.camunda.dmn.tester.shared.DmnEvalResult
import com.raquo.airstream.core.EventStream
import pme123.camunda.dmn.tester.shared.EvalStatus
import pme123.camunda.dmn.tester.shared.EvalStatus.INFO
import pme123.camunda.dmn.tester.shared.EvalStatus.WARN
import pme123.camunda.dmn.tester.shared.EvalStatus.ERROR
import be.doeraene.webcomponents.ui5.configkeys.IconName.`value-help`
import pme123.camunda.dmn.tester.client
import be.doeraene.webcomponents.ui5.configkeys.IconName.collapse

final case class CheckTheResults(
    private val testsAreRunningVar: Var[Boolean],
    private val selectedConfigsSignal: Signal[List[DmnConfig]]
):
  private lazy val hideSection =
    selectedConfigsSignal.map { selConfigs =>
      if (selConfigs.isEmpty)
        println(s"SELECTION CHANGED: ${selConfigs.size}")
        testsAreRunningVar.set(true)
      selConfigs.isEmpty
    }
  private lazy val comp =
    section(
      className := "App-section",
      hidden <-- hideSection,
      Card(
        cls := "medium",
        cls := "App-card",
        _.slots.header := Card.header(
          _.titleText := "3. Check the Test Results."
        )
      ),
      h3(),
      BusyIndicator(
        className := "busyIndication",
        _.active <-- testsAreRunningVar.signal,
        child <-- testResultsList
      )
    )

  private lazy val testResultsList = selectedConfigsSignal.signal
    .map { conf =>
      if (conf.nonEmpty)
        div(
          children <-- BackendClient
            .runTests(conf)
            .map {
              case Right(configs) =>
                handleTestResults(configs)
              case Left(error) =>
                Seq(errorMessage("Problem running Dmn Tests", error))
            }
        )
      else
        div()
    }

  private def handleTestResults(
      configs: Seq[Either[EvalException, DmnEvalResult]]
  ) = configs
    .map {
      case Right(result) => resultListItem(result)
      case Left(error)   => errorPanel(error)
    }
    .map { r =>
      testsAreRunningVar.set(false)
      r
    }

  private def resultListItem(result: DmnEvalResult): HtmlElement =
    val DmnEvalResult(
      dmn,
      inputKeys,
      outputKeys,
      evalResults,
      _
    ) = result
    val creator = RowCreator(result)
    val rows = creator.resultRows
    val msgCols = (inputKeys.size + outputKeys.size + 1)

    Panel(
      _.accessibleRole := PanelAccessibleRole.Complementary,
      className := "testResultsPanel",
      className := "flex-column",
      className := "full-width",
      _.collapsed := true,
      _.slots.header := Seq(
        h2(icon(result.maxEvalStatus), dmn.id),
        span(paddingLeft := "40px", dmn.dmnConfig.dmnPathStr)
      ),
      p(s"Hitpolicy: ${dmn.hitPolicy}"),
      p("DMN: " + dmn.dmnConfig.dmnPath.mkString("/")),
      p(
        "Variables: " + (if (dmn.dmnConfig.data.variables.nonEmpty)
                           dmn.dmnConfig.data.variables
                             .map(_.key)
                             .mkString(", ")
                         else "--")
      ),
      creator.errorRows,
      creator.noMatchingRows,
      creator.noMatchingInputs,
      creator.successful
    )
  end resultListItem

  private def errorPanel(evalException: EvalException) =
    val EvalException(decisionId, msg) = evalException
    Panel(
      className := "testResultsPanel",
      _.collapsed := true,
      _.slots.header := h2(icon(EvalStatus.ERROR), span(decisionId)),
      pre(padding := "6px", msg)
    )
  end errorPanel

object CheckTheResults:
  def apply(
      testsAreRunningVar: Var[Boolean],
      selectedConfigsSignal: Signal[List[DmnConfig]]
  ): ReactiveHtmlElement[html.Element] =
    new CheckTheResults(testsAreRunningVar, selectedConfigsSignal).comp

end CheckTheResults
