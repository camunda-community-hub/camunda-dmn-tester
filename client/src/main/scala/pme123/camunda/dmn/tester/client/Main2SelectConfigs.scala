package pme123.camunda.dmn.tester.client

import be.doeraene.webcomponents.ui5.*
import be.doeraene.webcomponents.ui5.configkeys.*
import com.raquo.airstream.core.{EventStream, Signal}
import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom
import org.scalajs.dom.{HTMLElement, html}
import pme123.camunda.dmn.tester.shared.DmnConfig

final case class Main2SelectConfigs(
                                basePathSignal: Signal[String],
                                dmnConfigsPathSignal: Signal[String],
                                selectedConfigsVar: Var[List[DmnConfig]],
                                dmnConfigsVar: Var[Seq[DmnConfig]]
):

  lazy val comp: ReactiveHtmlElement[html.Element] =
    section(
      className := "App-section",
      Card(
        cls := "medium",
        cls := "App-card",
        _.slots.header := Card.header(
          _.titleText <-- dmnConfigsPathSignal.map { path =>
            selectedConfigsVar.set(List.empty)
            s"2. Select the DMN Configurations you want to test in : $path"
          }
        ),
        div(child <-- dmnConfigs),
        Table(
          _.mode := TableMode.MultiSelect,
          _.slots.columns := Table.column(
            width := "30%",
            span("decisionId")
          ),
          _.slots.columns := Table.column(
            width := "60%",
            span("dmnPath")
          ),
          _.slots.columns := Table.column(""),
          _.slots.columns := Table.column(""),
          _.events.onSelectionChange.map(
            _.detail.selectedRows
              .map(r => allConfigsVar.now()(r.accessKey))
              .toList
          ) --> selectedConfigsVar,
          children <-- dmnConfigsVar.signal
            .map { configs =>
              allConfigsVar.set(configs.map { c => c.decisionId -> c }.toMap)
              configs
                .sortBy(_.decisionId)
                .map(config =>
                  Table.row(
                    accessKey := config.decisionId,
                    _.cell(config.decisionId),
                    _.cell(config.dmnPath.mkString("/")),
                    _.cell(
                      Button(
                        _.icon := IconName.edit,
                        _.tooltip := "Edit this DMN Configuration",
                        _.events.onClick.mapTo(true) --> openEditDialogBus,
                        _.events.onClick.mapTo(config) --> dmnConfigVar
                      )
                    ),
                    _.cell(
                      Button(
                        _.icon := IconName.delete,
                        _.tooltip := "Delete this DMN Configuration",
                        _.design := ButtonDesign.Negative,
                        _.events.onClick.mapTo(config) --> dmnConfigVar,
                        _.events.onClick
                          .map(_.target)
                          .map(Some(_)) --> openPopoverBus
                      )
                    )
                  )
                )
            }
        ),
        deletePopup,
        Button(
          _.icon := IconName.`add-document`,
          "Add Dmn Config",
          width := "98%",
          _.events.onClick.mapTo(true) --> openEditDialogBus,
          _.events.onClick.mapTo(DmnConfig()) --> dmnConfigVar
        )
      ),
      DmnConfigEditor(
        openEditDialogBus,
        basePathSignal,
        dmnConfigsPathSignal,
        dmnConfigVar,
        dmnConfigsVar
      )
    )

  private lazy val deletePopup =
    Popover(
      _.showAtFromEvents(openPopoverBus.events.collect { case Some(opener) =>
        opener
      }),
      _.closeFromEvents(openPopoverBus.events.collect { case None => () }),
      _.headerText := "Confirm Delete",
      p("Do you really want to remove the DMN Config:"),
      child <-- dmnConfigVar.signal.map(c => p(b(c.decisionId))),
      _.slots.footer := div(
        padding := "6px",
        div(flex := "1"),
        Button(
          className := "dialogButton",
          _.design := ButtonDesign.Negative,
          "Delete",
          _.events.onClick.mapTo(true) --> deleteConfigBus,
          _.events.onClick.mapTo(None) --> openPopoverBus.writer
        )
      ),
      div(hidden := true, child <-- deleteServiceEvents.map(_ => "ok"))
    )

  private lazy val dmnConfigs = dmnConfigsPathSignal
    .flatMap(BackendClient.getConfigs)
    .map {
      case Right(configs) =>
        dmnConfigsVar.set(configs)
        span("")
      case Left(error) =>
        errorMessage("Problem getting Dmn Configs", error)
    }

  private lazy val allConfigsVar = Var(Map.empty[String, DmnConfig])

  private lazy val openEditDialogBus: EventBus[Boolean] = new EventBus
  private lazy val openPopoverBus: EventBus[Option[HTMLElement]] = new EventBus
  private lazy val deleteConfigBus = EventBus[Boolean]()
  private lazy val dmnConfigVar: Var[DmnConfig] = Var(DmnConfig())

  private lazy val deleteServiceEvents = deleteConfigBus.events
    .withCurrentValueOf(dmnConfigVar.signal, dmnConfigsPathSignal)
    .flatMap { case (_, config, path) =>
      BackendClient
        .deleteConfig(config, path)
        .map { configs =>
          dmnConfigsVar.set(configs)
          "ok"
        }
    }
object Main2SelectConfigs:
  def apply(
             basePathSignal: Signal[String],
             dmnConfigsPathSignal: Signal[String],
             selectedConfigsVar: Var[List[DmnConfig]],
             dmnConfigsVar: Var[Seq[DmnConfig]]
  ): ReactiveHtmlElement[html.Element] =
    new Main2SelectConfigs(
      basePathSignal,
      dmnConfigsPathSignal,
      selectedConfigsVar,
      dmnConfigsVar
    ).comp

end Main2SelectConfigs
