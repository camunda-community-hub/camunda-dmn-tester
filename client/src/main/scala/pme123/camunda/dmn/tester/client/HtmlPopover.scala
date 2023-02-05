package pme123.camunda.dmn.tester.client

import be.doeraene.webcomponents.ui5.*
import be.doeraene.webcomponents.ui5.configkeys.*
import com.raquo.laminar.api.L.{*, given}

import org.scalajs.dom.HTMLElement

lazy val openPopoverBus: EventBus[(Option[HTMLElement], Any)] =
  new EventBus

def generalPopover(mods: Modifier[HtmlElement]*): HtmlElement =
    Popover(
      _.placementType := PopoverPlacementType.Bottom,
      _.showAtFromEvents(openPopoverBus.events.collect {
        case Some(opener) -> _ =>
          opener
      }),
      _.closeFromEvents(openPopoverBus.events.collect { case None -> _ =>
        ()
      }),

    ).amend(mods: _*)
