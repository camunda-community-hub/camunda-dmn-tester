package pme123.camunda.dmn.tester

import org.camunda.dmn.DmnEngine

object ResultPrinter {

  def printTestResult(
                       name: String,
                       inputs: Seq[String],
                       evaluated: Seq[RunResult]
                     ): Unit = {

    def formatStrings(strings: Seq[String]) = {
      val inputFormatter = "%1$20s"
      strings
        .map(i => inputFormatter.format(i.take(20)))
        .mkString("| ", " | ", " |")
    }

    def extractOutputs() = {
      evaluated
        .map {
          case RunResult(_, Right(DmnEngine.Result(rMap: Map[_, _]))) =>
            rMap.keySet.map(_.toString)
          case _ => Nil
        }
        .filter(_.nonEmpty)
        .take(1)
        .headOption
        .getOrElse(Nil)
        .toSeq
    }
    def formatResult(evalResult: DmnEngine.EvalResult) =
      evalResult match {
        case DmnEngine.Result(rMap: Map[_, _]) =>
          formatStrings(rMap.map { case (_, v) => v.toString }.toSeq)
        case DmnEngine.Result(other) => other.toString
        case DmnEngine.NilResult     => "NO RESULT"
      }

    //  println(s"*" * 100)
    println(name)
    println(
      s"EVALUATED: ${formatStrings(inputs)} -> ${formatStrings(extractOutputs())}"
    )
    //    println(s"*" * 100)
    def formatInputMap(inputMap: Map[String, Any]) = {
      formatStrings(inputs.map(k => inputMap(k).toString))
    }

    evaluated.foreach {
      case RunResult(inputMap, Right(DmnEngine.NilResult)) =>
        println(
          scala.Console.YELLOW +
            s"WARN:      ${formatInputMap(inputMap)} -> NO RESULT" +
            scala.Console.RESET
        )
      case RunResult(inputMap, Right(result)) =>
        println(
          s"INFO:      ${formatInputMap(inputMap)} -> ${formatResult(result)}"
        )
      case RunResult(inputMap, Left(failure)) =>
        println(
          scala.Console.RED +
            s"ERROR:     ${formatInputMap(inputMap)} -> $failure" +
            scala.Console.RESET
        )
    }
  }

}
