package pme123.scalably.slinky.todo

import pme123.scalably.slinky.shared.TodoItem
import slinky.core.FunctionalComponent
import slinky.core.annotations.react
import slinky.core.facade.ReactElement
import slinky.web.html._
import typings.antDesignIcons.components.AntdIcon
import typings.antDesignIconsSvg.mod._
import typings.antd.{antdStrings => aStr}
import typings.antd.components.{Col, _}
import typings.antd.listMod.{ListLocale, ListProps}
import typings.antd.paginationPaginationMod.PaginationConfig
import typings.antd.useFormMod
import typings.rcFieldForm.interfaceMod.BaseRule

import scala.scalajs.js
import scala.scalajs.js.Date

object components {

  @react object TList {

    case class Props(
                      todos: Seq[TodoItem],
                      onTodoToggle: TodoItem => Unit,
                      onTodoRemoval: TodoItem => Unit
                    )

    val component: FunctionalComponent[Props] = FunctionalComponent[Props] {
      props =>
        val Props(todos, onTodoToggle, onTodoRemoval) = props
        List
          .withProps(
            ListProps()
              .setDataSource(js.Array(todos: _*))
              .setLocale(
                ListLocale().setEmptyText(
                  "There's nothing to do :(".asInstanceOf[ReactElement]
                )
              )
              .setRenderItem((todo: TodoItem, _) =>
                TItem(todo, onTodoToggle, onTodoRemoval)
              )
              .setPagination(
                PaginationConfig()
                  .setPosition(aStr.bottom)
                  .setPageSize(10)
              )
          )
    }
  }

  @react object TItem {

    case class Props(
                      todo: TodoItem,
                      onTodoToggle: TodoItem => Unit,
                      onTodoRemoval: TodoItem => Unit
                    )

    val component: FunctionalComponent[Props] = FunctionalComponent[Props] {
      props =>
        val Props(todo, onTodoToggle, onTodoRemoval) = props
        List.Item
          .withKey(todo.id.get)
          .className("list-item")
          .actions(
            js.Array(
              Tooltip.TooltipPropsWithOverlayRefAttributes
                .titleReactElement(
                  if (todo.completed) "Mark as uncompleted"
                  else "Mark as completed")(
                  Switch
                    .checkedChildren(AntdIcon(CheckOutlined))
                    .unCheckedChildren(AntdIcon(CloseOutlined))
                    .onChange((_, _) => onTodoToggle(props.todo))
                    .defaultChecked(todo.completed)
                ),
              Popconfirm
                .title(
                  "Are you sure you want to delete?"
                    .asInstanceOf[ReactElement]
                )
                .onConfirm(_ => onTodoRemoval(todo))(
                  Button("X")
                    .className("remove-todo-button")
                    .`type`(aStr.primary)
                    .danger(true)
                )
            )
          )(
            div(
              className := "todo-item"
            )(
              Tag(todo.content)
                .color(if (todo.completed) aStr.cyan else aStr.red)
                .className("todo-tag")
            )
          )
    }
  }

  @react object AddTodoForm {

    case class Props(
                      onFormSubmit: TodoItem => Unit
                    )

    val component: FunctionalComponent[Props] = FunctionalComponent[Props] {
      props =>
        val Props(onFormSubmit) = props
        val form = useFormMod.default().head

        val onFinish = (_: Any) => {
          val content = form.getFieldValue("content").toString
          onFormSubmit(TodoItem(None, new Date().getTime().toInt, content))
          form.resetFields()
        }

        Form
          .form(form)
          .onFinish(onFinish)
          .layout(aStr.horizontal)
          .className("todo-form")(
            Row
              .gutter(20)(
                Col
                  .xs(24)
                  .sm(24)
                  .md(17)
                  .lg(19)
                  .xl(20)(
                    FormItem
                      .name("content")
                      .rulesVarargs(
                        BaseRule().setRequired(true)
                          .setMessage("Please say what you want to do!'")
                      )(
                        Input.placeholder("What needs to be done?")
                      )
                  ),
                Col
                  .xs(24)
                  .sm(24)
                  .md(7)
                  .lg(5)
                  .xl(4)(
                    Button("Add Todo")
                      .`type`(aStr.primary)
                      .htmlType(aStr.submit)
                      .block(true)
                      .icon(AntdIcon(PlusCircleFilled))
                  )
              )
          )
    }
  }

}
