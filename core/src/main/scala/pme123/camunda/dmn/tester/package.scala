package pme123.camunda.dmn

import org.camunda.dmn.DmnEngine

package object tester {
  case class RunResult(
      inputs: Map[String, Any],
      result: Either[DmnEngine.Failure, DmnEngine.EvalResult]
  )

  def formatStrings(strings: Seq[String]): String = {
    val inputFormatter = "%1$23s"
    strings
      .map{
        case i if i.length > 22 => inputFormatter.format(i.take(20) + "..")
        case i  => inputFormatter.format(i.take(22))

      }
      .mkString("| ", " | ", " |")
  }
}
