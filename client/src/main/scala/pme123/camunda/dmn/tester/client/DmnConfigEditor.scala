package pme123.camunda.dmn.tester.client

import be.doeraene.webcomponents.ui5.*
import be.doeraene.webcomponents.ui5.configkeys.*
import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.html
import pme123.camunda.dmn.tester.shared.*
import pme123.camunda.dmn.tester.shared.TesterValue.*

import scala.util.Random

final case class DmnConfigEditor(
    openEditDialogBus: EventBus[Boolean],
    basePathSignal: Signal[String],
    dmnConfigPathSignal: Signal[String],
    dmnConfigVar: Var[DmnConfig],
    dmnConfigsVar: Var[Seq[DmnConfig]]
):
  private lazy val dmnConfigSignal = dmnConfigVar.signal

  private lazy val saveConfigBus = EventBus[Boolean]()

  private lazy val comp = Dialog(
    _.showFromEvents(openEditDialogBus.events.filter(identity).mapTo(())),
    _.closeFromEvents(
      openEditDialogBus.events.map(!_).filter(identity).mapTo(())
    ),
    _.headerText := "DMN Config Editor",
    section(
      className := "editDialog",
      div(
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
          _.disabled <-- dmnConfigSignal
            .combineWith(dataInputsVar.signal)
            .map(c => c._1.hasErrors || c._2.exists(_.hasErrors)),
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
            c.data.inputs}{ inputs =>
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
        _.slots.columns := Table.column(
          "Base Path"
        ),
        _.slots.columns := Table.column(
          child.text <-- basePathSignal
        ),
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
          dmnConfigSignal
            .map { c => // work around - zoom not working
              dataInputsVar.set(c.data.inputs.map(_.withId))
              dataVariablesVar.set(c.data.variables.map(_.withId))
              c
            }
            .map(_.decisionId),
          decisionIdUpdater
        ),
        stringInputRow(
          "dmnPath",
          "Path to DMN",
          dmnConfigSignal.map(_.dmnPathError),
          dmnConfigSignal.map(_.dmnPathStr),
          dmnPathUpdater
        )
      ),
      div(
        child <-- dmnExistsStream.map {
          case Right(exists) if exists =>
            span("")
          case Right(_) =>
            div(icon(EvalStatus.WARN), "Be aware that this DMN does not exist.")
          case Left(error) =>
            errorMessage("Problem updating Dmn Config", error)
        }
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
      config.copy(testUnit = newValue)
    }
  private lazy val decisionIdUpdater =
    dmnConfigVar.updater[String] { (config, newValue) =>
      config.copy(decisionId = newValue)
    }
  private lazy val dmnExistsStream: EventStream[Either[String, Boolean]] =
    newPathBus
      .events
      .flatMap(path => BackendClient.validateDmnPath(path))

  private lazy val newPathBus: EventBus[String] = EventBus()
  private lazy val dmnPathUpdater: Observer[String] =
    dmnConfigVar.updater[String] { (config, newValue) =>
      newPathBus.emit(newValue)
      config.copy(dmnPath = newValue.split("/").toList)
    }

  private def inputValueVariableTables(inputsVar: Var[List[TesterInput]]) =
    def renderInputsTableRow(id: Int, inputSignal: Signal[TesterInput]) =
      def keyUpdater =
        inputsVar.updater[String] { (data, newValue) =>
          data.map(item =>
            if item.id.contains(id) then item.copy(key = newValue)
            else item
          )
        }
      def nullValueUpdater =
        inputsVar.updater[Boolean] { (data, newValue) =>
          data.map(item =>
            if item.id.contains(id) then
              item.copy(nullValue = newValue)
            else item
          )
        }
      def valuesUpdater =
        inputsVar.updater[String] { (data, newValue) =>
          data.map(item =>
            if (item.id.contains(id)) {
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
          s"key_$id",
          "Key of variable",
          inputSignal.map(_.keyError),
          inputSignal.map(_.key),
          keyUpdater
        ),
        _.cell(
          Input(
            _.id := s"valueType_$id",
            _.disabled := true,
            _.value <-- inputSignal.map(_.valueType)
          )
        ),
        _.stringInputCell(
          s"values_$id",
          "Values of variable to test (semicolon-separated)",
          inputSignal.map(_.valuesError),
          inputSignal.map(_.valuesAsString),
          valuesUpdater
        ),
        _.cell(
          CheckBox(
            _.id := s"nullValue_$id",
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
              inputsVar.update(_.filterNot(_.id.contains(id)))
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
      children <-- inputsVar.signal
        .split(_.id) { (id, _, inputSignal) =>
          renderInputsTableRow(id.get, inputSignal)
        }
    )
  end inputValueVariableTables

  private lazy val submitDmnConfig = saveConfigBus.events
    .withCurrentValueOf(dmnConfigSignal, dmnConfigPathSignal)
    .flatMap { case (_, config, path) =>
      val newConfig = config.copy(data =
        config.data.copy(
          inputs = dataInputsVar.now(),
          variables = dataVariablesVar.now()
        )
      )
      if (newConfig.hasErrors)
        EventStream.fromValue(
          errorMessage(
            "Validation Error(s)",
            "There are incorrect data, please correct them before saving."
          )
        )
      else {
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

object DmnConfigEditor:
  def apply(
      openEditDialogBus: EventBus[Boolean],
      basePathSignal: Signal[String],
      dmnConfigPathSignal: Signal[String],
      dmnConfigVar: Var[DmnConfig],
      dmnConfigsVar: Var[Seq[DmnConfig]]
  ): HtmlElement =
    new DmnConfigEditor(
      openEditDialogBus,
      basePathSignal,
      dmnConfigPathSignal,
      dmnConfigVar,
      dmnConfigsVar
    ).comp

end DmnConfigEditor
