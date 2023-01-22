package pme123.camunda.dmn.tester.client

import pme123.camunda.dmn.tester.shared.EvalStatus
import pme123.camunda.dmn.tester.shared.EvalStatus.*

import be.doeraene.webcomponents.ui5.*
import be.doeraene.webcomponents.ui5.configkeys.*
import com.raquo.laminar.api.L.{*, given}

def icon(status: EvalStatus) =
  val (name, backgroungColor) = status match
    case INFO  => (IconName.information, "green")
    case WARN  => (IconName.alert, "orange")
    case ERROR => (IconName.error, "red")
  Icon(
    _.name := name,
    marginRight := "1em",
    width := "1.5rem",
    height := "1.5rem",
    color := backgroungColor
  )

def errorMessage(title: String, error: String) =
  IllustratedMessage(
    _.name := IllustratedMessageType.ErrorScreen,
    _.titleText := title,
    _.slots.subtitle := div(
      if error.length < 100 then error else error.take(100) + "..."
    )
  )
