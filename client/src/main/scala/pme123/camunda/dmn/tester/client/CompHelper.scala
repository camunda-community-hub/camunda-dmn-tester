package pme123.camunda.dmn.tester.client

import be.doeraene.webcomponents.ui5.*
import be.doeraene.webcomponents.ui5.configkeys.*
import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom.HTMLElement
import pme123.camunda.dmn.tester.shared.*
import pme123.camunda.dmn.tester.shared.EvalStatus.*

def icon(status: EvalStatus) =
  val (name, backgroundColor) = status match
    case INFO  => (IconName.information, "green")
    case WARN  => (IconName.alert, "orange")
    case ERROR => (IconName.error, "red")
  Icon(
    _.name := name,
    marginRight := "1em",
    width := "1.5rem",
    height := "1.5rem",
    color := backgroundColor
  )

case class ErrorMessage(title: String, error: String)

def errorMessage(msg: ErrorMessage) =
  val ErrorMessage(title, error) = msg
  IllustratedMessage(
    width := "40rem",
    _.name := IllustratedMessageType.ErrorScreen,
    _.titleText := title,
    _.slots.subtitle := div(
      if error.length < 100 then error else error.take(100) + "..."
    )
  )

def stringInputRow(
    id: String,
    label: String,
    errorSignal: Signal[Option[String]],
    valueSignal: Signal[String],
    valueUpdater: Observer[String]
) =
  Table.row(
    _.cell(
      Label(
        className := "dialogLabel",
        _.forId := id,
        _.required := true,
        _.showColon := true,
        label
      )
    ),
    _.stringInputCell(
      id,
      label,
      errorSignal,
      valueSignal,
      valueUpdater,
      "700px"
    )
  )

def responseToHtml[T](
    body: T => HtmlElement
)(resp: Either[ErrorMessage, T]): HtmlElement =
  resp match
    case Right(value) =>
      body(value)
    case Left(error) =>
      errorMessage(error)

def panelHeader(dmnConfig: DmnConfig, evalStatus: EvalStatus) =
  Seq(
    h2(icon(evalStatus), dmnConfig.decisionId),
    span(paddingLeft := "40px", dmnConfig.dmnPathStr)
  )
extension (row: TableRow.type)
  def stringInputCell(
      id: String,
      placeholder: String,
      errorSignal: Signal[Option[String]],
      valueSignal: Signal[String],
      valueUpdater: Observer[String],
      inputWidth: String = "240px"
  ) =
    row.cell(
      Input(
        width := inputWidth,
        _.id := id,
        _.placeholder := placeholder,
        _.required := true,
        _.slots.valueStateMessage <-- errorSignal.map(_.toSeq.map(div(_))),
        _.valueState <-- errorSignal.map(
          _.map(_ => ValueState.Error)
            .getOrElse(ValueState.None)
        ),
        value <-- valueSignal,
        _.events.onInput.mapToValue --> valueUpdater
      )
    )
end extension
