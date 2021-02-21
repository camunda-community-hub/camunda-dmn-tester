package pme123.camunda.dmn.tester

package object shared {

  def asStrMap(valueMap: Map[String, TestedValue]) =
    valueMap.view.mapValues(_.value).toMap
}
