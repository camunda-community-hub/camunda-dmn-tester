package pme123.camunda.dmn.tester

package object shared {

  val TESTER_CONFIG_PATHS = "TESTER_CONFIG_PATHS"
  val STARTING_APP = "STARTING_APP"

  def asStrMap(valueMap: Seq[(String, TestedValue)]): Seq[(String, String)] =
    valueMap.map {
      case k -> v => k -> v.value
    }

  def asStrMap(valueMap: Map[String, Any]): Map[String, String] =
    valueMap.map {
      case k -> v => k -> v.toString
    }  
}
