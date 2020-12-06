package pme123.scalably.slinky.shared

import boopickle.Default._

sealed trait TodoPriority

case object TodoLow extends TodoPriority

case object TodoNormal extends TodoPriority

case object TodoHigh extends TodoPriority

case class TodoItem(
    id: Option[String],
    timeStamp: Int,
    content: String,
    priority: TodoPriority = TodoNormal,
    completed: Boolean = false
)

object TodoPriority {
  implicit val todoPriorityPickler: Pickler[TodoPriority] =
    generatePickler[TodoPriority]
}
