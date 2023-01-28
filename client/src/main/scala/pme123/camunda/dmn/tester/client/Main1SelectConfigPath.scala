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
    dmnConfigsPathVar: Var[String]
):
  private lazy val basePath = BackendClient.getBasePath

  private lazy val comp =
    section(
      className := "App-section",
      Card(
        cls := "medium",
        cls := "App-card",
        _.slots.header := Card.header(
          _.titleText := "1. Select Path where your DMN Configurations are."
        ),
        child <-- configPathEvents,
        div(
          className := "configPathsRow",
          Label(
            className := "configPathsSelect",
            "Base Path: ",
            child <-- basePath.map {
              case Right(path) =>
                basePathVar.set(path)
                span(path)
              case Left(error) =>
                div(
                  width := "40rem",
                  errorMessage("Problem getting Base Path", error)
                )
            }
          ),
          Select(
            className := "configPathsSelect",
            children <-- configuredPaths,
            _.events.onChange
              .map(_.detail.selectedOption.textContent) --> dmnConfigsPathVar
          ),
          newConfigPath
        )
      )
    )
  end comp

  private lazy val configPathsVar: Var[Seq[String]] = Var(Seq.empty[String])

  private lazy val configPathEvents
      : EventStream[ReactiveHtmlElement[html.Element]] =
    BackendClient.getConfigPaths
      .map {
        case Right(paths) =>
          configPathsVar.set(paths)
          if (dmnConfigsPathVar.now().isEmpty)
            dmnConfigsPathVar.set(paths.head)
          span("")
        case Left(error) =>
          errorMessage("Problem getting Dmn Config Paths", error)
      }

  private lazy val configuredPaths
      : Signal[Seq[ReactiveHtmlElement[html.Element]]] =
    configPathsVar.signal.map(
      _.map(p => {
        Select.option(
          value := p,
          p,
          _.selected <-- dmnConfigsPathVar.signal.map(p == _)
        )
      })
    )

  private lazy val newConfigPath =
    val newPathVar = Var("")

    div(
      className := "configPathsRow",
      Input(
        _.id := "selectFolder",
        className := "configPathsSelect",
        _.required := true,
        _.placeholder := "Add new path",
        _.events.onChange.mapToValue --> newPathVar,
        value <-- newPathVar.signal
      ),
      Button(
        _.icon := IconName.add,
        _.events.onClick.map { _ =>
          dmnConfigsPathVar.set(newPathVar.now())
          configPathsVar.set(
            configPathsVar.now() :+ newPathVar.now()
          )
          ""
        } --> newPathVar,
        "Add Path"
      )
    )
  end newConfigPath

object Main1SelectConfigPath:
  def apply(
      basePathVar: Var[String],
      dmnConfigsPathVar: Var[String]
  ): ReactiveHtmlElement[html.Element] =
    new Main1SelectConfigPath(basePathVar, dmnConfigsPathVar).comp
end Main1SelectConfigPath
