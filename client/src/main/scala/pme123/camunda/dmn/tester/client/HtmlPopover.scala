package pme123.camunda.dmn.tester.client

import be.doeraene.webcomponents.ui5.*
import be.doeraene.webcomponents.ui5.configkeys.*
import com.raquo.laminar.api.L.{*, given}

import org.scalajs.dom.HTMLElement


def generalPopover[T](openPopoverEvents: EventStream[(Option[HTMLElement], T)])(mods: Modifier[HtmlElement]*): HtmlElement =
    Popover(
      _.placementType := PopoverPlacementType.Bottom,
      _.showAtFromEvents(openPopoverEvents.collect {
        case Some(opener) -> _ =>
          opener
      }),
      _.closeFromEvents(openPopoverEvents.collect { case None -> _ =>
        ()
      }),

    ).amend(mods: _*)
