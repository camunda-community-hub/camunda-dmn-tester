package pme123.camunda.dmn.tester.client.runner

import autowire._
import boopickle.Default._
import org.scalablytyped.runtime.StringDictionary
import org.scalajs.dom.raw.HTMLInputElement
import pme123.camunda.dmn.tester.client.{basePathStr, col}
import pme123.camunda.dmn.tester.client.services.AjaxClient
import pme123.camunda.dmn.tester.shared.DmnApi
import slinky.core.FunctionalComponent
import slinky.core.annotations.react
import slinky.core.facade.Hooks.{useEffect, useState}
import slinky.web.html._
import typings.antDesignIcons.components.AntdIcon
import typings.antDesignIconsSvg.mod.{InboxOutlined, PlusOutlined}
import typings.antd.components._
import typings.antd.formFormMod.useForm
import typings.antd.mod.message
import typings.antd.{antdStrings => aStr}
import typings.rcFieldForm.interfaceMod.BaseRule
import typings.react.mod.{CSSProperties, ChangeEvent}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js.Dynamic.literal
import scala.scalajs.js.RegExp
import scala.util.{Failure, Success}

@react object ChangeConfigsForm {

  case class Props(
      basePath: String,
      onFormSubmit: String => Unit
  )

  val component: FunctionalComponent[Props] = FunctionalComponent[Props] {
    props =>
      val (paths, setPaths) = useState[Seq[String]](Seq.empty)
      val Props(basePath, onFormSubmit) = props
      val form = useForm().head

      def setPath(path: String): Unit = {
        form.setFieldsValue(
          StringDictionary(
            "path" -> path
          )
        )
        onFormSubmit(path)
      }

      lazy val onAddPath: String => Unit = { path =>
        setPaths(paths :+ path)
        setPath(path)
      }

      useEffect(
        () => {
          AjaxClient[DmnApi]
            .getConfigPaths()
            .call()
            .onComplete {
              case Success(paths) =>
                setPaths(paths)
                if (paths.nonEmpty)
                  setPath(paths.head)
              case Failure(ex) =>
                message.error(s"Problem loading Config Paths: ${ex.toString}")
            }
        },
        Seq.empty
      )

      Form
        .form(form)
        .layout(aStr.horizontal)
        .className("config-form")(
          Row
            .gutter(20)(
              col(
                PathSelect(basePath, paths, onAddPath, onFormSubmit)
              )
            )
        )
  }

}

@react object PathSelect {

  case class Props(
      basePath: String,
      configPaths: Seq[String],
      onAddPath: String => Unit,
      onChangePath: String => Unit
  )

  val component: FunctionalComponent[Props] = FunctionalComponent[Props] {
    props =>
      val Props(basePath, configPaths, onAddPath, onChangePath) = props
      val (name, setName) = useState[String]("")
      val (selected, setSelected) =
        useState[String](configPaths.headOption.getOrElse("-"))

      def onNameChange = (event: ChangeEvent[HTMLInputElement]) => {
        setName(event.currentTarget.value)
      }

      FormItem
        .name("path")
        .label(
          basePathStr(basePath)
        )(
          Select[String]
            .placeholder(
              "Select a path or add your own"
            )
            .dropdownRender(menu =>
              div(
                menu,
                Divider.style(CSSProperties().setMargin("4px 0")),
                div(
                  style := literal(
                    display = "flex",
                    flexWrap = "nowrap",
                    padding = 8
                  )
                )(
                  Input
                    .value(name)
                    .onChange(onNameChange),
                  a(
                    style := literal(
                      flex = "none",
                      padding = "8px",
                      display = "block",
                      cursor = "pointer"
                    ),
                    onClick := { _ =>
                      onAddPath(name)
                      setName("")
                    }
                  )(
                    AntdIcon(PlusOutlined),
                    "Add Path"
                  )
                )
              )
            )
            .onChange { (value, _) =>
              onChangePath(value)
            }(
              configPaths.map(p => Select.Option(p).withKey(p)(p))
            )
        )
  }
}
