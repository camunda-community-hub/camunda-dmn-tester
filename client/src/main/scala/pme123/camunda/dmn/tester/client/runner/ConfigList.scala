package pme123.camunda.dmn.tester.client.runner

import pme123.camunda.dmn.tester.client.runner.ConfigItem.activeCheck
import pme123.camunda.dmn.tester.client.{buttonWithTooltip, withTooltip}
import pme123.camunda.dmn.tester.shared.TesterValue.{
  BooleanValue,
  NumberValue,
  StringValue
}
import pme123.camunda.dmn.tester.shared.{DmnConfig, TesterData, TesterInput}
import slinky.core.FunctionalComponent
import slinky.core.WithAttrs.build
import slinky.core.annotations.react
import slinky.core.facade.Fragment
import slinky.core.facade.Hooks.useState
import slinky.web.html._
import typings.antDesignIcons.components.AntdIcon
import typings.antDesignIconsSvg.mod._
import typings.antd.antdStrings.circle
import typings.antd.components._
import typings.antd.listMod.{ListLocale, ListProps}
import typings.antd.{antdStrings => aStr}
import typings.rcFieldForm.interfaceMod.Store

import scala.scalajs.js
import scala.scalajs.js.Dynamic.literal
import scala.scalajs.js.JSON
import ujson.Value

@react object ConfigCard {

  case class Props(
      basePath: String,
      configs: Seq[DmnConfig],
      isLoaded: Boolean,
      maybeError: Option[String],
      setConfigs: Seq[DmnConfig] => Unit,
      onAddConfig: DmnConfig => Unit,
      onEditConfig: DmnConfig => Unit,
      onDeleteConfig: DmnConfig => Unit
  )

  val component: FunctionalComponent[Props] = FunctionalComponent[Props] {
    props =>
      val (isActive, setIsActive) = useState(false)
      val (isModalVisible, setIsModalVisible) = useState(false)
      val (maybeDmnConfig, setMaybeDmnConfig) =
        useState[Option[DmnConfig]](None)
      val Props(
        basePath,
        configs,
        isLoaded,
        maybeError,
        setConfigs,
        onAddConfig,
        onEditConfig,
        onDeleteConfig
      ) = props

      lazy val handleConfigToggle = { (config: DmnConfig) =>
        val newCF = config.copy(isActive = !config.isActive)
        setConfigs(configs.map {
          case c if c.decisionId == config.decisionId =>
            newCF
          case c => c
        })
      }

      lazy val editDmnConfig = (dmnConfig: DmnConfig) => {
        setMaybeDmnConfig(Some(dmnConfig))
        setIsModalVisible(true)
      }

      lazy val onSave = (values: Store) => {
        println(s"Received values of form: ${JSON.stringify(values)}")
        /*
           {"decisionId":"qwe","dmnPath":"qwe","testInputs":[{"type":"String","key":"qwe","values":"qwe"}],"variables":[{"type":"String","key":"kind","values":"Suisse"}]}
         */
        val json = ujson.read(JSON.stringify(values))

        val testerInputs = testInputsVars(json(testInputsKey))
        val variables = testInputsVars(json(variablesKey))
        setIsModalVisible(false)
        val dmnPath = json("pathOfDmn").str
          .split("/")
          .map(_.trim)
          .filter(_.nonEmpty)
          .toList
        val dmnConfig = DmnConfig(
          json("decisionId").str,
          TesterData(testerInputs, variables),
          dmnPath
        )
        maybeDmnConfig
          .map(existingConfig =>
            onEditConfig( // take existing Test Cases
              dmnConfig.copy(data =
                dmnConfig.data.copy(testCases = existingConfig.data.testCases)
              )
            )
          )
          .getOrElse(onAddConfig(dmnConfig))

      }

      Card
        .withKey("selectConfigCard")
        .title(
          Fragment
            .withKey("selectConfigFragment")(
              "2. Select the DMN Configurations you want to test.",
              div(style := literal(textAlign = "right", marginRight = 10))(
                activeCheck(
                  isActive = isActive,
                  active => {
                    setIsActive(active)
                    setConfigs(configs.map(_.copy(isActive = active)))
                  }
                )
              ),
              DmnConfigForm(
                basePath,
                maybeDmnConfig,
                isModalVisible,
                (store: Store) => onSave(store),
                () => setIsModalVisible(false)
              )
            )
        )
        .actions(
          js.Array(
            buttonWithTooltip(
              FileAddOutlined,
              "Create new DMN Configuration.",
              () => {
                setMaybeDmnConfig(None)
                setIsModalVisible(true)
              }
            )
          )
        )(
          (maybeError, isLoaded) match {
            case (Some(msg), _) =>
              Alert
                .message(
                  s"Error: The DMN Configurations could not be loaded. (is the path ok?)"
                )
                .`type`(aStr.error)
                .showIcon(true)
            case (_, false) =>
              Spin
                .size(aStr.default)
                .spinning(true)(
                  Alert
                    .message("Loading Configs")
                    .`type`(aStr.info)
                    .showIcon(true)
                )
            case _ =>
              ConfigList(
                configs.sortBy(_.dmnPath.toString).sortBy(_.decisionId),
                editDmnConfig,
                onDeleteConfig,
                handleConfigToggle
              )
          }
        )
  }

  private def testInputsVars(json: Value.Value) = {
    json.arr.map { e =>
      val values = e("values").str
        .split(",")
        .map(_.trim)
        .filter(_.nonEmpty)

      val testerValues = e("type").str match {
        case "String" => values.map(StringValue)
        case "Number" => values.map(NumberValue.apply)
        case "Boolean" => values.map(BooleanValue.apply)
      }
      TesterInput(e("key").str, testerValues.toList)
    }.toList
  }
}

@react object ConfigList {

  case class Props(
      configs: Seq[DmnConfig],
      onConfigEdit: DmnConfig => Unit,
      onConfigDelete: DmnConfig => Unit,
      onConfigToggle: DmnConfig => Unit
  )

  val component: FunctionalComponent[Props] = FunctionalComponent[Props] {
    props =>
      val Props(configs, onConfigEdit, onConfigDelete, onConfigToggle) = props
      List
        .withProps(
          ListProps()
            .setDataSource(js.Array(configs: _*))
            .setLocale(
              ListLocale().setEmptyText(
                Empty().description("There are no DMN configurations:(").build
              )
            )
            .setRenderItem((config: DmnConfig, _) =>
              ConfigItem(config, onConfigEdit, onConfigDelete, onConfigToggle)
            )
        )
  }
}

@react object ConfigItem {

  case class Props(
      config: DmnConfig,
      onConfigEdit: DmnConfig => Unit,
      onConfigDelete: DmnConfig => Unit,
      onConfigToggle: DmnConfig => Unit
  )

  val component: FunctionalComponent[Props] = FunctionalComponent[Props] {
    props =>
      val Props(config, onConfigEdit, onConfigDelete, onConfigToggle) = props
      List.Item
        .withKey(config.decisionId)
        .className("list-item")
        .actions(
          js.Array(
            withTooltip(
              s"Edit '${config.decisionId}''",
              Button
                .shape(circle)
                .icon(AntdIcon(EditOutlined))
                .onClick(_ => onConfigEdit(config))
            ),
            Popconfirm
              .title(
                build(
                  p(s"Are you sure you want to delete '${config.decisionId}'?")
                )
              )
              .onConfirm(_ => onConfigDelete(config))(
                withTooltip(
                  s"Delete '${config.decisionId}''",
                  Button
                    .shape(circle)
                    .icon(AntdIcon(DeleteOutlined))
                )
              ),
            withTooltip(
              if (config.isActive) "Mark as inactive"
              else "Mark as active",
              activeCheck(config.isActive, _ => onConfigToggle(config))
            )
          )
        )(
          div(
            className := "config-item"
          )(
            Tag(s"${config.decisionId} -> ${config.dmnPath.mkString("/")}")
              .color(if (config.isActive) aStr.blue else aStr.red)
              .className("config-tag")
          )
        )
  }

  def activeCheck(
      isActive: Boolean,
      onConfigToggle: Boolean => Unit
  ) = {
    Switch
      .checkedChildren(AntdIcon(CheckOutlined))
      .unCheckedChildren(AntdIcon(CloseOutlined))
      .onChange((active, _) => onConfigToggle(active))
      .checked(isActive)
  }
}
