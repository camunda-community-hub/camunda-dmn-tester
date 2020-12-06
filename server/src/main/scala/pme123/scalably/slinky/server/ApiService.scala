package pme123.scalably.slinky.server

import java.nio.ByteBuffer
import java.util.{Date, UUID}

import boopickle.Default._
import boopickle.UnpickleImpl
import pme123.scalably.slinky.shared._

import scala.concurrent.ExecutionContext.global

class ApiService extends Api {
  var todos = Seq(
    TodoItem(Option("41424344-4546-4748-494a-4b4c4d4e4f50"), 0x61626364, "Wear shirt that says “Life”. Hand out lemons on street corner.", TodoLow, completed = false),
    TodoItem(Option("2"), 0x61626364, "Make vanilla pudding. Put in mayo jar. Eat in public.", TodoNormal, completed = false),
    TodoItem(Option("3"), 0x61626364, "Walk away slowly from an explosion without looking back.", TodoHigh, completed = false),
    TodoItem(Option("4"), 0x61626364, "Sneeze in front of the pope. Get blessed.", TodoNormal, completed = true)
  )

  override def welcomeMsg(name: String): String =
    s"Welcome to SPA, $name! Time is now ${new Date}"

  override def getAllTodos(): Seq[TodoItem] = {
    // provide some fake Todos
    Thread.sleep(300)
    println(s"Sending ${todos.size} Todo items")
    todos
  }

  // update a Todo
  override def updateTodo(item: TodoItem): Seq[TodoItem] = {
    // TODO, update database etc :)
    if(item.id.nonEmpty) {
      todos = todos.collect {
        case i if i.id == item.id => item
        case i => i
      }
      println(s"Todo item was updated: $item")
    } else {
      // add a new item
      val newItem = item.copy(id = Some(UUID.randomUUID().toString))
      todos :+= newItem
      println(s"Todo item was added: $newItem")
    }
    Thread.sleep(300)
    todos
  }

  // delete a Todo
  override def deleteTodo(itemId: String): Seq[TodoItem] = {
    println(s"Deleting item with id = $itemId")
    Thread.sleep(300)
    todos = todos.filterNot(_.id.contains(itemId))
    todos
  }
}
