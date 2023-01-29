package pme123.camunda.dmn.tester.client

import be.doeraene.webcomponents.ui5.*
import be.doeraene.webcomponents.ui5.configkeys.*
import be.doeraene.webcomponents.ui5.configkeys.IconName.{
  `value-help`,
  collapse
}
import com.raquo.airstream.core.{EventStream, Signal}
import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.{HTMLElement, html}
import pme123.camunda.dmn.tester.client
import pme123.camunda.dmn.tester.shared.EvalStatus.{ERROR, INFO, WARN}
import pme123.camunda.dmn.tester.shared.HandledTesterException.EvalException
import pme123.camunda.dmn.tester.shared.{DmnConfig, DmnEvalResult, EvalStatus}

final case class Main3CheckTheResults(
    private val testsAreRunningVar: Var[Boolean],
    private val dmnConfigPathSignal: Signal[String],
    private val selectedConfigsSignal: Signal[List[DmnConfig]],
    private val dmnConfigsVar: Var[Seq[DmnConfig]],
):
  private lazy val hideSection =
    selectedConfigsSignal.map { selConfigs =>
      if (selConfigs.isEmpty)
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
      case Right(result) => EvalResultsPanel(result, dmnConfigPathSignal, dmnConfigsVar)
      case Left(error)   => errorPanel(error)
    }
    .map { r =>
      testsAreRunningVar.set(false)
      r
    }

  private def errorPanel(evalException: EvalException) =
    val EvalException(dmnConfig, msg) = evalException
    Panel(
      className := "testResultsPanel",
      _.collapsed := true,
      _.slots.header := panelHeader(dmnConfig, EvalStatus.ERROR),
      div(padding := "6px", msg.split("\n").map {
        case v if v.startsWith(">") => li(v.replace("> ", ""))
        case v => p(v)
      }.toSeq)
    )
  end errorPanel

object Main3CheckTheResults:
  def apply(
      testsAreRunningVar: Var[Boolean],
      dmnConfigPathSignal: Signal[String],
      selectedConfigsSignal: Signal[List[DmnConfig]],
      dmnConfigsVar: Var[Seq[DmnConfig]],
  ): ReactiveHtmlElement[html.Element] =
    new Main3CheckTheResults(
      testsAreRunningVar,
      dmnConfigPathSignal,
      selectedConfigsSignal,
      dmnConfigsVar
    ).comp

end Main3CheckTheResults
