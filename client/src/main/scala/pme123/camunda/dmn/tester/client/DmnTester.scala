package pme123.camunda.dmn.tester.client

import pme123.camunda.dmn.tester.shared.*

import com.raquo.laminar.api.L.{*, given}
import be.doeraene.webcomponents.ui5.*
import org.scalajs.dom
import be.doeraene.webcomponents.ui5.configkeys.*
import java.awt.Event

import pme123.camunda.dmn.tester.client.Main3CheckTheResults
@main
def DmnTester(): Unit =
  /* renderOnDomContentLoaded(
    dom.document.getElementById("app"),
    Main.appElement()
  )*/
  renderOnDomContentLoaded(
    dom.document.getElementById("dmnTester"),
    Main.dmnTester()
  )
end DmnTester

object Main:

  private lazy val selectedPathVar = Var("")
  private lazy val selectedConfigsVar = Var(List.empty[DmnConfig])
  private lazy val testsAreRunningVar: Var[Boolean] = Var(false)
  private lazy val dmnConfigsVar: Var[Seq[DmnConfig]] =
    Var[Seq[DmnConfig]](Seq.empty)
  
  def dmnTester() =
    div(
      MainHeader(),
      SelectConfigPath(selectedPathVar),
      SelectConfigs(selectedPathVar.signal, selectedConfigsVar, dmnConfigsVar),
      Main3CheckTheResults(testsAreRunningVar, selectedPathVar.signal, selectedConfigsVar.signal, dmnConfigsVar),
      MainFooter(),
    )

end Main
