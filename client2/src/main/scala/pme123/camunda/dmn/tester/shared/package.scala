package pme123.camunda.dmn.tester

package object shared {

  def asStrMap(valueMap: Seq[(String, TestedValue)]) =
    valueMap.map {
      case k -> v => k -> v.value
    }
}
