package pme123.camunda.dmn.tester.client

import be.doeraene.webcomponents.ui5.*
import be.doeraene.webcomponents.ui5.configkeys.*
import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.html

final case class Main5Footer():

  lazy val comp: ReactiveHtmlElement[html.Element] =
    section(
        className := "App-footer",
        "Check it out on Github: ",
        a(
          href := "https://github.com/pme123/camunda-dmn-tester",
          "camunda-dmn-tester"
        )
      )
  end comp

object Main5Footer:
    def apply(): ReactiveHtmlElement[html.Element] =
        new Main5Footer().comp  

end Main5Footer
