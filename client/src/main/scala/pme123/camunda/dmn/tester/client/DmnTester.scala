package pme123.camunda.dmn.tester.client

import pme123.camunda.dmn.tester.shared.*
import com.raquo.laminar.api.L.{*, given}
import be.doeraene.webcomponents.ui5.*
import org.scalajs.dom
import be.doeraene.webcomponents.ui5.configkeys.*
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.html
import pme123.camunda.dmn.tester.client.Main3CheckTheResults

@main
def DmnTester(): Unit =
  renderOnDomContentLoaded(
    dom.document.getElementById("dmnTester"),
    Main.dmnTester()
  )
end DmnTester

object Main:

  private lazy val basePathVar = Var("")
  private lazy val basePathSignal = basePathVar.signal
  private lazy val dmnConfigsPathVar = Var("")
  private lazy val selectedConfigsVar = Var(List.empty[DmnConfig])
  private lazy val testsAreRunningVar: Var[Boolean] = Var(false)
  private lazy val dmnConfigsVar: Var[Seq[DmnConfig]] =
    Var[Seq[DmnConfig]](Seq.empty)
  
  def dmnTester(): ReactiveHtmlElement[html.Div] =
    div(
      Main0Header(),
      Main1SelectConfigPath(basePathVar, dmnConfigsPathVar),
      Main2SelectConfigs(basePathSignal, dmnConfigsPathVar.signal, selectedConfigsVar, dmnConfigsVar),
      Main3CheckTheResults(testsAreRunningVar, dmnConfigsPathVar.signal, selectedConfigsVar.signal, dmnConfigsVar),
      MainFooter(),
    )

end Main
