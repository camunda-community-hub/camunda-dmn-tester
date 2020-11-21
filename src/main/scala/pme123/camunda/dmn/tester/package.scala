package pme123.camunda.dmn

import org.camunda.dmn.DmnEngine

package object tester {
  case class RunResult(
                        inputs: Map[String, Any],
                        result: Either[DmnEngine.Failure, DmnEngine.EvalResult]
                      )
}
