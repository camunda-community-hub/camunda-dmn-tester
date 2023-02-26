package pme123.camunda.dmn.tester.client

import be.doeraene.webcomponents.ui5.*
import be.doeraene.webcomponents.ui5.configkeys.*
import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.{HTMLElement, html}
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

  private lazy val comp = Dialog(
    _.showFromEvents(openEditDialogBus.events.filter(identity).mapTo(())),
    _.closeFromEvents(
      openEditDialogBus.events.map(!_).filter(identity).mapTo(())
    ),
    _.headerText := "DMN Config Editor",
    section(
      className := "editDialog",
      child <-- submitDmnConfig,
      editDmnConfigForm
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
    ),
    generalPopover(openPopoverBus.events)(
      child <-- openPopoverBus.events.collect {
        case _ -> help => p(help)
      }
    )
  )

  // state
  private lazy val dataInputsVar: Var[List[TesterInput]] = Var(List.empty)
  private lazy val dataVariablesVar: Var[List[TesterInput]] = Var(List.empty)
  private lazy val dmnConfigSignal = dmnConfigVar.signal

  // events
  private lazy val saveConfigBus = EventBus[Boolean]()
  private lazy val newPathBus: EventBus[String] = EventBus()
  private given openPopoverBus: EventBus[(Option[HTMLElement], String)] = new EventBus

  private lazy val dmnExistsEvents =
    newPathBus.events
      .flatMap(path =>
        BackendClient
          .validateDmnPath(path)
      )

  private lazy val testUnitUpdater =
    dmnConfigVar.updater[Boolean] { (config, newValue) =>
      config.copy(testUnit = newValue)
    }
  private lazy val acceptMissingRulesUpdater =
    dmnConfigVar.updater[Boolean] { (config, newValue) =>
      config.copy(acceptMissingRules = newValue)
    }
  private lazy val decisionIdUpdater =
    dmnConfigVar.updater[String] { (config, newValue) =>
      config.copy(decisionId = newValue)
    }
  private lazy val dmnPathUpdater: Observer[String] =
    dmnConfigVar.updater[String] { (config, newValue) =>
      newPathBus.emit(newValue)
      config.copy(dmnPath = newValue.split("/").toList)
    }

  // components
  private lazy val editDmnConfigForm =
    form(
      className := "configDialogForm",
      editDmnConfigTable,
      div(
        className := "editTitle",
        paddingLeft :="100px",
        child <-- dmnExistsEvents.map {
          case Right(exists) if exists =>
            span("")
          case Right(_) =>
            Title(
              _.level := TitleLevel.H5,
              icon(EvalStatus.WARN),
              "Be aware that this DMN does not exist."
            )
          case Left(error) =>
            errorMessage(error)
        }
      ),
      div(
        className := "editTitle",
        Title(_.level := TitleLevel.H4, "Test Inputs ")
      ),
      inputValueVariableTables(dataInputsVar),
      Button(
        _.icon := IconName.add,
        "Add Test Input",
        width := "100%",
        _.events.onClick --> (_ => dataInputsVar.update(_ :+ TesterInput()))
      ),
      div(
        className := "editTitle",
        Title(
          _.level := TitleLevel.H4,
          "Test Variables used in Inputs and Outputs"
        )
      ),
      inputValueVariableTables(dataVariablesVar),
      Button(
        _.icon := IconName.add,
        "Add Variable Input",
        width := "100%",
        _.events.onClick --> (_ => dataVariablesVar.update(_ :+ TesterInput()))
      )
    )

  private def editDmnConfigTable =
    Table(
      className := "dialogTable",
      _.slots.columns := Table.column(
        "Base Path"
      ),
      _.slots.columns := Table.column(
        child.text <-- basePathSignal
      ),
      booleanInputRow(
        "testUnit",
        "Test is Unit",
        "Check if you want test your DMN independently.",
        dmnConfigSignal.map(_.testUnit),
        testUnitUpdater
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
      ),
      booleanInputRow(
        "acceptMissingRules",
        "Accept Missing Rules",
        "If you have lots of possible inputs and you only want to test a few of them.",
        dmnConfigSignal.map(_.acceptMissingRules),
        acceptMissingRulesUpdater
      ),
    )

  private def inputValueVariableTables(inputsVar: Var[List[TesterInput]]) =
    def renderInputsTableRow(id: Int, inputSignal: Signal[TesterInput]) =

      def updater[A](copyFunction: (TesterInput, A) => TesterInput) =
        inputsVar.updater[A] { (data, newValue) =>
          data.map(item =>
            if item.id.contains(id) then copyFunction(item, newValue)
            else item
          )
        }

      def keyUpdater =
        updater[String]((item, newValue) => item.copy(key = newValue))
      def nullValueUpdater =
        updater[Boolean]((item, newValue) => item.copy(nullValue = newValue))
      def valuesUpdater =
        updater[String]((item, newValue) =>
          if (item.id.contains(id)) {
            val values = newValue
              .split(",")
              .map(_.trim)
              .filter(_.nonEmpty)
            val testerValues = values.map(TesterValue.fromString)
            item.copy(values = testerValues.toList)
          } else item
        )

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
            ErrorMessage(
              "Validation Error(s)",
              "There are incorrect data, please correct them before saving."
            )
          )
        )
      else
        BackendClient
          .updateConfig(newConfig, path)
          .map(responseToHtml(configs => {
            dmnConfigsVar.set(configs)
            openEditDialogBus.emit(false)
            span("")
          }))
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
