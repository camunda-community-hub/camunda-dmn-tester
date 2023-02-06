package pme123.camunda.dmn.tester.client

import be.doeraene.webcomponents.ui5.*
import be.doeraene.webcomponents.ui5.configkeys.*
import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.html

object Main0Header:

  def apply(): ReactiveHtmlElement[html.Element] =
    section(
      cls("topSection"),
      img(
        src("logo.png"),
        className := "App-logo",
        alt := "logo"
      ),
      Title(
        className := "App-title",
        _.level := TitleLevel.H1,
        "Camunda DMN Table Tester V2 BETA \uD83C\uDF89"
      )
    )
  end apply

end Main0Header
