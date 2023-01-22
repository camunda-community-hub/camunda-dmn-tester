package pme123.camunda.dmn.tester.client

import pme123.camunda.dmn.tester.shared.*

import com.raquo.laminar.api.L.{*, given}
import be.doeraene.webcomponents.ui5.*
import org.scalajs.dom
import be.doeraene.webcomponents.ui5.configkeys.*
import java.awt.Event

import pme123.camunda.dmn.tester.client.CheckTheResults
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

  val selectedPathVar = Var("")
  val selectedConfigsVar = Var(List.empty[DmnConfig])
  val testsAreRunningVar: Var[Boolean] = Var(false)

  def dmnTester() =
    div(
      MainHeader(),
      SelectConfigPath(selectedPathVar),
      SelectConfigs(selectedPathVar.signal, selectedConfigsVar),
      CheckTheResults(testsAreRunningVar, selectedConfigsVar.signal),
      MainFooter(),
    )

end Main
