package pme123.camunda.dmn.tester.client

import be.doeraene.webcomponents.ui5.*
import be.doeraene.webcomponents.ui5.configkeys.*
import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.html

final case class MainHeader():

  lazy val comp: ReactiveHtmlElement[html.Element] =
    section(
      cls("topSection"),
      img(
        src("logo.png"),
        className := "App-logo",
        alt := "logo"
      ),
      h1(cls("App-title"), "Camunda DMN Table Tester")
    )
  end comp

object MainHeader:
    def apply(): ReactiveHtmlElement[html.Element] =
        new MainHeader().comp  

end MainHeader
