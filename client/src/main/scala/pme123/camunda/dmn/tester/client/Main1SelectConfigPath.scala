package pme123.camunda.dmn.tester.client

import be.doeraene.webcomponents.ui5.*
import be.doeraene.webcomponents.ui5.configkeys.*
import com.raquo.airstream.core.{EventStream, Signal, Source}
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom
import org.scalajs.dom.html

final case class Main1SelectConfigPath(
                                        basePathVar: Var[String],
                                        dmnConfigPathVar: Var[String]
):
  private lazy val basePath = BackendClient.getBasePath

  private lazy val comp =
    section(
      className := "App-section",
      Card(
        cls := "App-card",
        _.slots.header := Card.header(
          _.titleText := "1. Select Path where your DMN Configurations are."
        ),
        div(
          className := "configPathsRow",
          basePathLabel,
          dmnConfigPathsSelect,
          newConfigPathDiv
        ),
        child <-- configPathEvents, // needed to start fetching
      )
    )
  end comp

  private lazy val configPathsVar: Var[Seq[String]] = Var(Seq.empty[String])

  private lazy val configPathEvents
      : EventStream[ReactiveHtmlElement[html.Element]] =
    BackendClient.getConfigPaths
      .map(responseToHtml(paths => {
        configPathsVar.set(paths)
        if (dmnConfigPathVar.now().isEmpty)
          dmnConfigPathVar.set(paths.head)
        span("")
      }))

  private lazy val basePathLabel =
    Label(
      className := "configPathsSelect",
      "Base Path: ",
      child <-- basePath.map(responseToHtml(path => {
        basePathVar.set(path)
        span(path)
      }))
    )
    
  private lazy val dmnConfigPathsSelect =
    Select(
      className := "configPathsSelect",
      children <-- configuredPaths,
      _.events.onChange
        .map(_.detail.selectedOption.textContent) --> dmnConfigPathVar
    )
    
  private lazy val configuredPaths
      : Signal[Seq[ReactiveHtmlElement[html.Element]]] =
    configPathsVar.signal.map(
      _.map(p => {
        Select.option(
          value := p,
          p,
          _.selected <-- dmnConfigPathVar.signal.map(p == _)
        )
      })
    )

  private lazy val newConfigPathDiv =
    val newPathVar = Var("")

    div(
      className := "configPathsRow",
      Input(
        className := "configPathsSelect",
        _.required := true,
        _.placeholder := "Add new path",
        value <-- newPathVar.signal,
        _.events.onChange.mapToValue --> newPathVar
      ),
      Button(
        _.icon := IconName.add,
        _.events.onClick.map { _ =>
          dmnConfigPathVar.set(newPathVar.now())
          configPathsVar.update(existing =>
            existing :+ newPathVar.now()
          )
          ""
        } --> newPathVar,
        "Add Path"
      )
    )
  end newConfigPathDiv

object Main1SelectConfigPath:
  def apply(
      basePathVar: Var[String],
      dmnConfigsPathVar: Var[String]
  ): ReactiveHtmlElement[html.Element] =
    new Main1SelectConfigPath(basePathVar, dmnConfigsPathVar).comp
end Main1SelectConfigPath
