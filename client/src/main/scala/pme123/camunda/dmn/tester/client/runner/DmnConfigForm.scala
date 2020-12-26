package pme123.camunda.dmn.tester.client.runner

import boopickle.Default._
import pme123.camunda.dmn.tester.client._
import slinky.core.{FunctionalComponent, TagMod}
import slinky.core.annotations.react
import slinky.core.facade.Fragment
import slinky.web.html.p
import typings.antDesignIcons.components.AntdIcon
import typings.antDesignIconsSvg.mod.{MinusCircleOutlined, PlusOutlined}
import typings.antd.antdStrings.{add, baseline, horizontal}
import typings.antd.components._
import typings.antd.formFormMod.useForm
import typings.antd.formListMod.{FormListFieldData, FormListOperation}
import typings.rcFieldForm.interfaceMod.{BaseRule, Store}
import typings.react.mod.CSSProperties

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js

@react object DmnConfigForm {

  case class Props(
      isModalVisible: Boolean,
      onCreate: Store => Unit,
      onCancel: () => Unit
  )

  val component: FunctionalComponent[Props] = FunctionalComponent[Props] {
    props =>
      val Props(isModalVisible, onCreate, onCancel) = props
      val form = useForm().head

      Modal
        .title("Create new DMN Config")
        .okText("Create")
        .width(1000)
        .visible(isModalVisible)
        .onOk(_ =>
          form
            .validateFields()
            .toFuture
            .map { (values: Store) =>
              form.resetFields()
              onCreate(values)
              values
            }
        )
        .onCancel(_ => onCancel())(
          Form
            .form(form)
            .className("config-form")(
              FormItem
                .name("decisionId")
                .label(
                  textWithTooltip(
                    "Decision Id",
                    "The Id of the DMN Table you want to test."
                  )
                )
                .rulesVarargs(
                  BaseRule()
                    .setRequired(true)
                    .setMessage("The decision Id is required!")
                )(
                  Input()
                ),
              FormItem
                .name("dmnPath")
                .label(
                  textWithTooltip(
                    "DMN Path",
                    "The relative Path of the DMN to the Base Path. Example: core/src/test/resources/numbers.dmn"
                  )
                )
                .rulesVarargs(
                  BaseRule()
                    .setRequired(true)
                    .setMessage("The DMN Path is required!")
                )(
                  Input()
                ),
              p("Test Inputs"),
              FormList(
                children = (
                    fields: js.Array[FormListFieldData],
                    op: FormListOperation
                ) =>
                  Fragment(
                    fields.map { field =>
                      Row
                        .withKey(field.key.toString)(
                          Col
                            .style(CSSProperties().setPaddingRight(10))
                            .span(7)(
                              FormItem
                                .label(
                                  textWithTooltip(
                                    "Key",
                                    "The input variable key needed in the DMN Table."
                                  )
                                )
                                .name(field.name + "Key")
                                .fieldKey(field.fieldKey + "Key")
                                .rulesVarargs(
                                  BaseRule()
                                    .setRequired(true)
                                    .setMessage(
                                      "The Test Input Key is required!"
                                    )
                                )(
                                  Input.id(field.fieldKey + "Key")()
                                )
                            ),
                          Col
                            .style(CSSProperties().setPaddingRight(10))
                            .span(16)(
                              FormItem
                                .label(
                                  textWithTooltip(
                                    "Values",
                                    "All inputs for this input you want to test. Example: ch,fr,de,CH"
                                  )
                                )
                                .name(field.name + "Values")
                                .fieldKey(field.fieldKey + "Values")
                                .rulesVarargs(
                                  BaseRule()
                                    .setRequired(true)
                                    .setMessage(
                                      "The Test Input Values is required!"
                                    )
                                )(
                                  Input.id(field.fieldKey + "Values")()
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
                        .build
                    }.toSeq :+
                      FormItem
                        .name("addInput")(
                          buttonWithTextTooltip(
                            PlusOutlined,
                            "Add Input",
                            "Add an Input of the DMN Table.",
                            () => op.add()
                          )
                        )
                        .build: _*
                  ),
                name = "testInputs"
              )
            )
        )
  }

}
