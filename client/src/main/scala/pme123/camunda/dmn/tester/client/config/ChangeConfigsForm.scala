package pme123.camunda.dmn.tester.client.config

import org.scalajs.dom.raw.HTMLInputElement
import slinky.core.annotations.react
import slinky.core.facade.Hooks.useState
import slinky.core.{FunctionalComponent, SyntheticEvent}
import slinky.web.html._
import typings.antDesignIcons.components.AntdIcon
import typings.antDesignIconsSvg.mod.{InboxOutlined, PlusOutlined}
import typings.antd.components._
import typings.antd.mod.message
import typings.antd.{antdStrings => aStr}
import typings.react.mod.{CSSProperties, ChangeEvent}

import scala.scalajs.js.Dynamic.literal

@react object ChangeConfigsForm {

  case class Props(
      basePath: String,
      onFormSubmit: String => Unit
  )

  val component: FunctionalComponent[Props] = FunctionalComponent[Props] {
    props =>
      val Props(basePath, onFormSubmit) = props

      Form
        .layout(aStr.horizontal)
        .className("config-form")(
          Row
            .gutter(20)(
              Col.span(24)(
                PathSelect(basePath, onFormSubmit)
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
      onChangePath: String => Unit
  )

  val component: FunctionalComponent[Props] = FunctionalComponent[Props] {
    props =>
      val (paths, setPaths) = useState[Seq[String]](configPaths)
      val (name, setName) = useState[String]("")
      val Props(basePath, onChangePath) = props

      def onNameChange = (event: ChangeEvent[HTMLInputElement]) => {
        setName(event.currentTarget.value)
      }

      def addPath = (_: SyntheticEvent[_, _]) => {
        setPaths(paths :+ name)
        setName("")
      }

      FormItem
        .name("path")
        .label(if(basePath.length > 40) ".." + basePath.takeRight(40) else basePath)
        .initialValue(paths.head)(
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
                    onClick := addPath
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
              paths.map(p => Select.Option(p).withKey(p)(p))
            )
        )
  }
}
