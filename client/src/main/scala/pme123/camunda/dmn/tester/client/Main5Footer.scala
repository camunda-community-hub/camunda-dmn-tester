package pme123.camunda.dmn.tester.client

import be.doeraene.webcomponents.ui5.*
import be.doeraene.webcomponents.ui5.configkeys.*
import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.html

object Main5Footer:

  def apply(): ReactiveHtmlElement[html.Element] =
    section(
      className := "App-footer",
      Label(
        "Check it out on Github: ",
        Link(
          href := "https://github.com/pme123/camunda-dmn-tester",
          "camunda-dmn-tester"
        )
      ),
      br(),
      Label(
        "UI powered by ",
        Link(href := "https://laminar.dev", "Laminar"),
        " and ",
        Link(
          href := "https://github.com/sherpal/LaminarSAPUI5Bindings",
          "Laminar bindings for SAP ui5 web-components"
        ),
        "."
      )
    )
  end apply


end Main5Footer
