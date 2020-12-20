package pme123.camunda.dmn.tester.client.runner

import autowire._
import boopickle.Default._
import org.scalablytyped.runtime.StringDictionary
import org.scalajs.dom.raw.HTMLInputElement
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
import typings.react.mod.{CSSProperties, ChangeEvent}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js.Dynamic.literal
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

      def onAddPath(path: String): Unit = {
        setPaths(paths :+ path)
        println(s"SetFieldsValue: $path")
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
              Col.span(24)(
                PathSelect(basePath, paths, onAddPath, onFormSubmit)
              )
            ),
          Row
            .gutter(20)(
              Col.span(24)(
                // UploadElement()
              )
            )
        )
  }

}

@react object UploadElement {

  case class Props(
  )

  val component: FunctionalComponent[Props] = FunctionalComponent[Props] { _ =>
    Upload
      .name("file")
      .multiple(false)
      .action("https://www.mocky.io/v2/5cc8019d300000980a055e76")
      .onChange { info =>
        info.file.status.toString match {
          case "uploading" =>
            println(s"Uploading ${info.file}")
          case "done" =>
            message
              .success(s"${info.file.name} file uploaded successfully.")
          case "error" =>
            message.error(s"${info.file.name} file upload failed.")
        }
      }(
        p(className := "ant-upload-drag-icon")(AntdIcon(InboxOutlined)),
        p(className := "ant-upload-text")(
          "Click or drag a DMN Config to this area to upload"
        ),
        p(className := "ant-upload-hint")("Support for a single upload.")
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

      println(s"Component called: $selected")
      def onNameChange = (event: ChangeEvent[HTMLInputElement]) => {
        setName(event.currentTarget.value)
      }

      FormItem
        .name("path")
        .label(
          if (basePath.length > 40) ".." + basePath.takeRight(40) else basePath
        )(
          Select[String]
            .value(selected)
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
