package pme123.camunda.dmn.tester.client

import be.doeraene.webcomponents.ui5.*
import be.doeraene.webcomponents.ui5.configkeys.*
import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.html
import pme123.camunda.dmn.tester.shared.*
import pme123.camunda.dmn.tester.shared.TesterValue.*

final case class DmnConfigEditor(
    openEditDialogBus: EventBus[Boolean],
    dmnConfigPathSignal: Signal[String],
    dmnConfigVar: Var[DmnConfig],
    dmnConfigsVar: Var[Seq[DmnConfig]]
):
  lazy val dmnConfigSignal = dmnConfigVar.signal

  lazy val saveConfigBus = EventBus[Boolean]()

  lazy val comp = Dialog(
    _.showFromEvents(openEditDialogBus.events.filter(identity).mapTo(())),
    _.closeFromEvents(
      openEditDialogBus.events.map(!_).filter(identity).mapTo(())
    ),
    _.headerText := "DMN Config Editor",
    section(
      className := "editDialog",
      div(
        // hidden := true,
        child <-- submitDmnConfig
      ),
      configForm
    ),
    p(""),
    _.slots.footer := section(
      padding := "10px",
      textAlign := "right",
      width := "100%",
      div(
        width := "100%",
        textAlign := "right",
        Button(
          className := "dialogButton",
          "Cancel",
          _.events.onClick.mapTo(false) --> openEditDialogBus.writer
        ),
        Button(
          className := "dialogButton",
          _.design := ButtonDesign.Emphasized,
          disabled <-- dmnConfigSignal.map(_.hasErrors),
          "Save",
          _.events.onClick.mapTo(true) --> saveConfigBus
        )
      )
    )
  )

  private lazy val configForm =
    form(
      className := "configDialogForm",
      /*    onMountCallback{ ctx =>
          dmnConfigVar.zoom{ c =>
            println(s"c.data.inputs: ${c.data.inputs}")
            c.data.inputs}{ inputs =>
             println(s"back c.data.inputs: ${inputs}")
             val dmnConfig = dmnConfigVar.now()
             dmnConfig.copy(data = dmnConfig.data.copy(inputs = inputs))
          }(ctx.owner) --> dataInputsVar
          dmnConfigVar.zoom(_.data.variables){ variables =>
             val dmnConfig = dmnConfigVar.now()
             dmnConfig.copy(data = dmnConfig.data.copy(variables = variables))
          }(ctx.owner) --> dataVariablesVar
      },*/
      Table(
        className := "dialogTable",
        Table.row(
          title := "Check if you want test your DMN independently.",
          _.cell(
            Label(
              className := "dialogLabel",
              _.forId := "testUnit",
              _.required := true,
              "Test is Unit"
            )
          ),
          _.cell(
            CheckBox(
              _.id := "testUnit",
              _.checked <-- dmnConfigSignal.map(_.testUnit),
              _.events.onChange.map(_.target.checked) --> testUnitUpdater
            )
          )
        ),
        stringInputRow(
          "decisionId",
          "Decision Id",
          dmnConfigSignal.map(_.decisionIdError),
          dmnConfigSignal.map { c => // work around - zoom not working
            dataInputsVar.set(c.data.inputs)
            dataVariablesVar.set(c.data.variables)
            c
          }.map(_.decisionId),
          decisionIdUpdater
        ),
        stringInputRow(
          "dmnPath",
          "Path to DMN",
          dmnConfigSignal.map(_.dmnPathError),
          dmnConfigSignal.map(_.dmnPathStr),
          dmnPathUpdater
        ),
      ),
      h4("Test Inputs "),
      inputValueVariableTables(dataInputsVar),
      Button(
        _.icon := IconName.add,
        "Add Test Input",
        width := "100%",
        _.events.onClick --> (_ => dataInputsVar.update(_ :+ TesterInput()))
      ),
      h4("Test Variables used in Inpts and Outputs"),
      inputValueVariableTables(dataVariablesVar),
      Button(
        _.icon := IconName.add,
        "Add Variable Input",
        width := "100%",
        _.events.onClick --> (_ => dataVariablesVar.update(_ :+ TesterInput()))
      )
    )

  private lazy val dataInputsVar: Var[List[TesterInput]] = Var(List.empty)
  private lazy val dataVariablesVar: Var[List[TesterInput]] = Var(List.empty)

  private lazy val testUnitUpdater =
    dmnConfigVar.updater[Boolean] { (config, newValue) =>
      println(s"NEW VALUE: $newValue")
      config.copy(testUnit = newValue)
    }
  private lazy val decisionIdUpdater =
    dmnConfigVar.updater[String] { (config, newValue) =>
      config.copy(decisionId = newValue)
    }
  private lazy val dmnPathUpdater: Observer[String] =
    dmnConfigVar.updater[String] { (config, newValue) =>
      config.copy(dmnPath = newValue.split("/").toList)
    }

  private def inputValueVariableTables(inputsVar: Var[List[TesterInput]]) =
    def renderInputsTableRow(key: String, inputSignal: Signal[TesterInput]) =
      println(s"RENDERED: $key")
      def keyUpdater =
        inputsVar.updater[String] { (data, newValue) =>
          data.map(item =>
            if item.key == key then item.copy(key = newValue) else item
          )
        }
      def nullValueUpdater =
        inputsVar.updater[Boolean] { (data, newValue) =>
          data.map(item =>
            if item.key == key then item.copy(nullValue = newValue) else item
          )
        }
      def valueUpdater =
        inputsVar.updater[String] { (data, newValue) =>
          data.map(item =>
            if (item.key == key) {
              val values = newValue
                .split(",")
                .map(_.trim)
                .filter(_.nonEmpty)
              val testerValues = values.map(TesterValue.fromString)
              item.copy(values = testerValues.toList)
            } else item
          )
        }

      Table.row(
          _.stringInputCell(
            "dmnPath",
            "Path to DMN",
            inputSignal.map(_.keyError),
            inputSignal.map(_.key),
            keyUpdater
          ),
        _.cell(
          Input(
            _.id := s"valueType_$key",
            _.readonly := true,
            value <-- inputSignal.map(_.valueType)
          )
        ),
        _.cell(
          Input(
            _.id := s"valuesAsString_$key",
            _.placeholder := "Input Test Values",
            _.required := true,
            value <-- inputSignal.map(_.valuesAsString),
            _.events.onChange.mapToValue --> valueUpdater
          )
        ),
        _.cell(
          CheckBox(
            _.id := s"nullValue_$key",
            _.checked <-- inputSignal.map(_.nullValue),
            _.events.onChange.map(_.target.checked) --> nullValueUpdater
          )
        ),
        _.cell(
          Button(
            _.icon := IconName.delete,
            _.design := ButtonDesign.Negative,
            _.tooltip := "Delete this entry.",
            _.events.onClick --> (_ =>
              inputsVar.update(_.filterNot(_.key == key))
            )
          )
        )
      )

    Table(
      _.slots.columns := Table.column("Key"),
      _.slots.columns := Table.column("Type"),
      _.slots.columns := Table.column("Values"),
      _.slots.columns := Table.column("Null value"),
      _.slots.columns := Table.column(""),
      children <-- inputsVar.signal.split(_.key) { (key, _, inputSignal) =>
        renderInputsTableRow(key, inputSignal)
      }
    )
  end inputValueVariableTables

  private def inputForString(
      valueSignal: Signal[String],
      valueUpdater: Observer[String]
  ) =
    Input(
      _.required := true,
      controlled(
        value <-- valueSignal,
        onInput.mapToValue --> valueUpdater
      )
    )

  private lazy val submitDmnConfig = saveConfigBus.events
    .withCurrentValueOf(dmnConfigSignal, dmnConfigPathSignal)
    .flatMap { case (_, config, path) =>
      println(s"CHECK: ${config.hasErrors} - ${config.decisionIdError}")
      if (config.hasErrors)
        EventStream.fromValue(
          errorMessage(
            "Validation Error(s)",
            "There are incorrect data, please correct them before saving."
          )
        )
      else {
        val newConfig = config.copy(data =
          config.data.copy(
            inputs = dataInputsVar.now(),
            variables = dataVariablesVar.now()
          )
        )
        BackendClient
          .updateConfig(newConfig, path)
          .map {
            case Right(configs) =>
              dmnConfigsVar.set(configs)
              openEditDialogBus.emit(false)
              span("")
            case Left(error) =>
              errorMessage("Problem updating Dmn Config", error)
          }
      }
    }
end DmnConfigEditor
