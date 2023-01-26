package pme123.camunda.dmn.tester

package object shared {

  def asStrMap(valueMap: Seq[(String, TestedValue)]) =
    valueMap.map {
      case k -> v => k -> v.value
    }

  def asStrMap(valueMap: Map[String, Any]) =
    valueMap.map {
      case k -> v => k -> v.toString
    }  
}
