package pme123.camunda.dmn.tester

package object shared {

  val TESTER_CONFIG_PATHS = "TESTER_CONFIG_PATHS"
  val STARTING_APP = "STARTING_APP"

  def asStrMap(valueMap: Seq[(String, TestedValue)]): Seq[(String, String)] =
    valueMap.map {
      case k -> v =>
        println(s"valueMap: Seq[(String, TestedValue)]: ${k} -> ${v.getClass} - $v")
        k -> v.value
    }

  def asStrMap(valueMap: Map[String, Any]): Map[String, String] =
    valueMap.map {
      case k -> v =>
        println(s"valueMap: Map[String, Any]: ${k} -> ${v.getClass} - $v")
        k -> v.toString
    }
}
