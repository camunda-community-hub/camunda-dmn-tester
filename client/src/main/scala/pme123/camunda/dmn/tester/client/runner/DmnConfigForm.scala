package pme123.camunda.dmn.tester.client.runner

import boopickle.Default._
import org.scalablytyped.runtime.StringDictionary
import pme123.camunda.dmn.tester.client._
import pme123.camunda.dmn.tester.shared.{DmnConfig, TesterInput}
import slinky.core.FunctionalComponent
import slinky.core.WithAttrs.build
import slinky.core.annotations.react
import slinky.core.facade.Fragment
import slinky.core.facade.Hooks.useEffect
import slinky.web.html.p
import typings.antDesignIconsSvg.mod.{MinusCircleOutlined, PlusOutlined}
import typings.antd.components._
import typings.antd.formFormMod.useForm
import typings.antd.formListMod.{FormListFieldData, FormListOperation}
import typings.rcFieldForm.interfaceMod.{BaseRule, Store}
import typings.react.mod.CSSProperties

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js
import scala.scalajs.js.RegExp

@react object DmnConfigForm {

  case class Props(
      basePath: String,
      maybeDmnConfig: Option[DmnConfig],
      isModalVisible: Boolean,
      onSave: Store => Unit,
      onCancel: () => Unit
  )

  val component: FunctionalComponent[Props] = FunctionalComponent[Props] {
    props =>
      val Props(basePath, maybeDmnConfig, isModalVisible, onSave, onCancel) =
        props
      val form = useForm().head

      useEffect(
        () => {
          if (isModalVisible)
            form.setFieldsValue(
              StringDictionary(
                "testUnit" -> maybeDmnConfig.forall(_.testUnit),
                "decisionId" -> s"${maybeDmnConfig.map(_.decisionId).getOrElse("")}",
                "pathOfDmn" -> s"${maybeDmnConfig.map(_.dmnPath.mkString("/")).getOrElse("")}",
                testInputsKey -> inputVariablesDictionary(
                  maybeDmnConfig.toList.flatMap(_.data.inputs)
                ),
                variablesKey -> inputVariablesDictionary(
                  maybeDmnConfig.toList.flatMap(_.data.variables)
                )
              )
            )
        },
        Seq(isModalVisible)
      )

      Modal
        .title("DMN Config Editor")
        .okText(
          textWithTooltip(
            "Save",
            "Persist your changes. Existing TestCases are not lost."
          )
        )
        .width(1000)
        .visible(isModalVisible)
        .forceRender(true)
        .onOk(_ =>
          form
            .validateFields()
            .toFuture
            .map { (values: Store) =>
              form.resetFields()
              onSave(values)
              values
            }
        )
        .onCancel(_ => onCancel())(
          Form
            .form(form)
            .className("config-form")(
              FormItem()
                .withKey("testUnitKey")
                .name("testUnit")
                .label(
                  textWithTooltip(
                    "Test is Unit",
                    "Check if you want test your DMN independently."
                  )
                )
                .valuePropName("checked")(
                Checkbox()
              ),
              FormItem
                .withKey("decisionIdKey")
                .name("decisionId")
                .label(
                  textWithTooltip(
                    "Decision Id",
                    "The Id of the DMN Table you want to test. Valid examples: my-dmn, my_dmn3"
                  )
                )
                .rulesVarargs(identifierRule)(
                  Input()
                ),
              FormItem
                .name("pathOfDmn") // dmnPath did not work!?
                .withKey("pathOfDmnKey")
                .label(
                  textWithTooltip(
                    s"DMN Path $basePath",
                    s"The relative Path of the DMN to the Base Path. Example: core/src/test/resources/numbers.dmn"
                  )
                )
                .rulesVarargs(
                  BaseRule()
                    .setRequired(true)
                    .setPattern(RegExp("""^[^\/](.+\/)*.+\.dmn$""", "gm"))
                    .setMessage(
                      "The DMN Path is required in the following format: path/to/dmn/my.dmn!"
                    )
                )(
                  Input()
                ),
              p("Test Inputs"),
              testInputForm(testInputsKey, "Test Input"),
              p("Test Variables used in Inpts and Outputs"),
              testInputForm(variablesKey, "Variable")
            )
        )
  }

  private def inputVariablesDictionary(inputsOrVars: List[TesterInput]) = {
    js.Array(
      inputsOrVars.map { case ti @ TesterInput(key, nullValue, _) =>
        StringDictionary(
          "key" -> key,
          "type" -> ti.valueType,
          "nullValue" -> nullValue,
          "values" -> ti.valuesAsString
        )
      }: _*
    )
  }

  private def testInputForm(key: String, label: String) = {
    FormList(
      children = (
          fields: js.Array[FormListFieldData],
          op: FormListOperation
      ) => {
        Fragment.withKey(s"${key}Key")(
          fields.map { field =>
            Row
              .withKey(field.key.toString)(
                Col
                  .style(CSSProperties().setPaddingRight(10))
                  .span(5)(
                    FormItem
                      .withKey(field.fieldKey + "key")
                      .label(
                        textWithTooltip(
                          "Key",
                          "The key used in the DMN Table."
                        )
                      )
                      .nameVarargs(field.name, "key")
                      .fieldKeyVarargs(field.fieldKey + "key")
                      .rulesVarargs(identifierRule)(
                        Input()
                      )
                  ),
                Col
                  .style(CSSProperties().setPaddingRight(10))
                  .span(4)(
                    FormItem
                      .withKey(field.fieldKey + "type")
                      .label(
                        textWithTooltip(
                          "Type",
                          s"The type of your $label."
                        )
                      )
                      .initialValue("String")
                      .nameVarargs(field.name, "type")
                      .fieldKeyVarargs(field.fieldKey + "type")(
                        Select[String].apply(
                          Select.Option("String")("String"),
                          Select.Option("Number")("Number"),
                          Select.Option("Boolean")("Boolean")
                        )
                      )
                  ),
                Col
                  .style(CSSProperties().setPaddingRight(10))
                  .span(11)(
                    FormItem
                      .withKey(field.fieldKey + "values")
                      .label(
                        textWithTooltip(
                          "Values",
                          "All values for this variable you want to test. Example: ch,fr,de,CH. No Apostrophs \" needed!"
                        )
                      )
                      .fieldKeyVarargs(field.fieldKey + "values")
                      .nameVarargs(field.name, "values")
                      .rulesVarargs(
                        BaseRule()
                          .setRequired(true)
                          .setPattern(
                            RegExp("""^[^,]+(,[^,]+)*$""", "gm")
                          )
                          .setMessage(
                            "The Test Input Values is required!"
                          )
                      )(
                        Input()
                      )
                  ),
                Col.span(3)(
                  FormItem
                    .withKey(field.fieldKey + "nullValue")
                    .label(
                      textWithTooltip(
                        "Null value",
                        s"Check if you want to test a null value."
                      )
                    )
                    .valuePropName("checked")
                    .nameVarargs(field.name, "nullValue")
                    .fieldKeyVarargs(field.fieldKey + "nullValue")(
                      Checkbox.apply()
                    )
                ),
                Col.span(1)(
                  iconWithTooltip(
                    MinusCircleOutlined,
                    "Delete this Test Input.",
                    () => op.remove(field.name)
                  )
                )
              )
          }.toSeq :+
            FormItem
              .withKey("addInput")
              .name("addInput")(
                buttonWithTextTooltip(
                  PlusOutlined,
                  s"Add $label",
                  s"Add $label of the DMN Table.",
                  () => op.add()
                )
              )
        )
      },
      name = key
    )
  }

  private val identifierRule = BaseRule()
    .setRequired(true)
    .setPattern(RegExp("""^[^\d\W][-\w]+$""", "gm"))
    .setMessage("This is required and must a valid Identifier!")

}
