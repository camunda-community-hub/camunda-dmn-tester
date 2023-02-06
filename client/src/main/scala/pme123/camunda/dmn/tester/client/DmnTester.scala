package pme123.camunda.dmn.tester.client

import be.doeraene.webcomponents.ui5.*
import be.doeraene.webcomponents.ui5.configkeys.*
import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom
import org.scalajs.dom.html
import pme123.camunda.dmn.tester.client.Main3CheckTheResults
import pme123.camunda.dmn.tester.shared.*

@main
def DmnTester(): Unit =
  renderOnDomContentLoaded(
    dom.document.getElementById("dmnTester"),
    Main()
  )
end DmnTester

object Main:

  // shared state
  private lazy val basePathVar = Var("")
  private lazy val dmnConfigPathVar = Var("")
  private lazy val dmnConfigsVar: Var[Seq[DmnConfig]] =
    Var[Seq[DmnConfig]](Seq.empty)
  private lazy val selectedConfigsVar = Var(List.empty[DmnConfig])

  def apply(): ReactiveHtmlElement[html.Div] =
    div(
      Main0Header(),
      Main1SelectConfigPath(basePathVar, dmnConfigPathVar),
      Main2SelectConfigs(
        basePathVar.signal,
        dmnConfigPathVar.signal,
        selectedConfigsVar,
        dmnConfigsVar
      ),
      Main3CheckTheResults(
        dmnConfigPathVar.signal,
        selectedConfigsVar.signal,
        dmnConfigsVar
      ),
      Main5Footer()
    )

end Main
