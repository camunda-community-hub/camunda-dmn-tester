package pme123.scalably.slinky.todo

import autowire.{clientCallable, _}
import boopickle.Default._
import org.scalajs.dom.XMLHttpRequest
import org.scalajs.dom.ext.AjaxException
import pme123.scalably.slinky.services.AjaxClient
import pme123.scalably.slinky.shared.{Api, TodoItem}
import pme123.scalably.slinky.todo.components.{AddTodoForm, TList}
import slinky.core.FunctionalComponent
import slinky.core.facade.Hooks.{useEffect, useState}
import slinky.web.html.{div, s}
import typings.antd.antdStrings
import typings.antd.antdStrings.{center, middle}
import typings.antd.components._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport
import scala.util.{Failure, Success}

@JSImport("resources/Todo.css", JSImport.Default)
@js.native
object TodoCSS extends js.Object

object containers {
  private val css = TodoCSS

  val TodoContainer: FunctionalComponent[Unit] = FunctionalComponent[Unit] {
    _ =>
      val (error, setError) = useState[Option[String]](None)
      val (isLoaded, setIsLoaded) = useState(false)
      val (todos, setItems) = useState(Seq.empty[TodoItem])

      // Note: the empty deps array [] means
      // this useEffect will run once
      useEffect(        () =>
          AjaxClient[Api].getAllTodos().call().onComplete {
            case Success(todos) =>
              setIsLoaded(true)
              setItems(todos)
            case Failure(ex) =>
              setIsLoaded(true)
              setError(Some(ex.toString))
          },
        Seq.empty
      )

      lazy val handleFormSubmit =
        (todo: TodoItem) =>
          AjaxClient[Api].updateTodo(todo).call().foreach { todos =>
            setItems(todos)
          }

      lazy val handleTodoToggle =
        (todo: TodoItem) =>
          AjaxClient[Api].updateTodo(todo.copy(completed = !todo.completed)).call().foreach { todos =>
            setItems(todos)
          }
      lazy val handleRemoveTodo =
        (todo: TodoItem) =>
          AjaxClient[Api].deleteTodo(todo.id.get).call().foreach { todos =>
            setItems(todos)
          }
      Row
        // .gutter(20) //[o, 20] ?
        .justify(center)
        .className("todos-container")
        .align(middle)(
          Col
            .xs(23)
            .sm(23)
            .md(21)
            .lg(20)
            .xl(18)(
              PageHeader
                .title("Add Todo")
                .subTitle(
                  "To add a todo, just fill the form below and click in add todo."
                )
            ),
          Col
            .xs(23)
            .sm(23)
            .md(21)
            .lg(20)
            .xl(18)(
              Card
                .title("Create a new todo")(AddTodoForm(handleFormSubmit))
            ),
          Col
            .xs(23)
            .sm(23)
            .md(21)
            .lg(20)
            .xl(18)(
              Card
                .title("Todo List")(
                  (error, isLoaded) match {
                    case (Some(msg), _) =>
                      Alert
                        .message(s"Error: $msg")
                        .`type`(antdStrings.error)
                        .showIcon(true)
                    case (_, false) =>
                      Spin
                        .size(antdStrings.default)
                        .spinning(true)(
                          Alert
                            .message("Loading Todos")
                            .`type`(antdStrings.info)
                            .showIcon(true)
                        )
                    case _ =>
                      TList(todos, handleTodoToggle, handleRemoveTodo)
                  }
                )
            )
        )
  }
}
