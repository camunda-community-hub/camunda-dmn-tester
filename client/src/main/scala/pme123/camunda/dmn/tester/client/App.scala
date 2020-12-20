package pme123.camunda.dmn.tester.client

import pme123.camunda.dmn.tester.client.runner.containers
import slinky.core._
import slinky.core.annotations.react
import slinky.web.html._
import typings.antd.components._

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

@JSImport("resources/App.css", JSImport.Default)
@js.native
object AppCSS extends js.Object

@JSImport("antd/dist/antd.css", JSImport.Default)
@js.native
object AntCSS extends js.Any

@JSImport("resources/logo.png", JSImport.Default)
@js.native
object ReactLogo extends js.Object

@react object App {
  type Props = Unit

  //noinspection ScalaUnusedSymbol
  private val appCss = AppCSS
  //noinspection ScalaUnusedSymbol
  private val antCss = AntCSS

  val component: FunctionalComponent[Props] = FunctionalComponent[Props] { _ =>

    Layout(className := "App")(
      Layout.Header(className := "App-header")(
        img(
          src := ReactLogo.asInstanceOf[String],
          className := "App-logo",
          alt := "logo"
        ),
        h1(className := "App-title")("Camunda DMN Table Tester")
      ),
      Layout.Content(
        containers.DmnConfigContainer()
      ),
      Layout.Footer(className := "App-footer")(
        "Check it out on Github: ",
        a(href := "https://github.com/pme123/camunda-dmn-tester")(
          "camunda-dmn-tester"
        )
      )
    )
  }
}
