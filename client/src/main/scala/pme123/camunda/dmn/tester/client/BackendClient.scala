package pme123.camunda.dmn.tester.client

import pme123.camunda.dmn.tester.shared.*
import io.circe.*
import io.circe.syntax.*
import io.circe.generic.auto.*

import com.raquo.airstream.web.AjaxEventStream
import com.raquo.airstream.web.AjaxEventStream.AjaxStreamError
import com.raquo.laminar.api.L.*
import org.scalajs.dom
import org.scalajs.*
import pme123.camunda.dmn.tester.shared.HandledTesterException.EvalException
import scala.scalajs.js.typedarray.TypedArrayBuffer
import scala.scalajs.js.typedarray.ArrayBuffer
import com.raquo.airstream.core.EventStream
import org.scalajs.dom.XMLHttpRequest
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object BackendClient {

  private lazy val url =
    if (dom.window.location.port == "5173") {
      println(
        "ATTENTION: CORS - see https://stackoverflow.com/a/38000615/2750966"
      )
      "http://localhost:8883"
    } else ""

  // update DmnConfig
  def updateConfig(
      dmnConfig: DmnConfig,
      path: String
  ): EventStream[Either[String, Seq[DmnConfig]]] =
    println(s"updateConfig: $path $dmnConfig")
    AjaxEventStream
      .put(
        s"$url/api/dmnConfig?configPath=${URLEncoder.encode(path, StandardCharsets.UTF_8)}",
        data = dmnConfig.asJson.toString
      )
      .map(req =>
        parser
          .parse(req.responseText)
          .flatMap(_.as[Seq[DmnConfig]])
          .left
          .map(exc =>
            s"Problem parsing body: ${req.responseText}\n${exc.getMessage()}"
          )
      )
      .recover(err =>
        Some(Left(s"Problem updating Dmn Config: ${err.getMessage()} "))
      )

  // delete DmnConfig
  def deleteConfig(
      dmnConfig: DmnConfig,
      path: String
  ): EventStream[Seq[DmnConfig]] =
    println(s"deleteConfig: $path $dmnConfig")
    AjaxEventStream
      .delete(
        s"$url/api/dmnConfig?configPath=${URLEncoder.encode(path, StandardCharsets.UTF_8)}",
        data = dmnConfig.asJson.toString
      )
      .map { req =>
        //     println(s"RESULT ${dmnConfig.decisionId}: ${req.responseText}")
        req
      }
      .map(req =>
        parser
          .parse(req.responseText)
          .flatMap(_.as[Seq[DmnConfig]])
          .getOrElse(Seq.empty)
      )

  // gets the absolute path of the server / or where you run the testRunner.sc Script
  def getBasePath: EventStream[Either[String, String]] =
    AjaxEventStream
      .get(s"$url/api/basePath")
      .map(r => Right(r.responseText))
      .recover(err =>
        Some(Left(s"Problem getting Base Paths: ${err.getMessage} "))
      )

  // get configured Paths
  def getConfigPaths: EventStream[Either[String, Seq[String]]] =
    AjaxEventStream
      .get(s"$url/api/configPaths")
      .map(req =>
        parser
          .parse(req.responseText)
          .flatMap(_.as[Seq[String]])
          .left
          .map(exc =>
            s"Problem parsing body: ${req.responseText}\n${exc.getMessage}"
          )
      )
      .recover(err =>
        Some(Left(s"Problem getting Config Paths: ${err.getMessage} "))
      )

    // get DmnConfigs items
  def getConfigs(path: String): EventStream[Either[String, Seq[DmnConfig]]] =
    AjaxEventStream
      .get(
        s"$url/api/dmnConfigs?configPath=${URLEncoder.encode(path, StandardCharsets.UTF_8)}"
      )
      .map(req =>
        parser
          .parse(req.responseText)
          .flatMap(_.as[Seq[DmnConfig]])
          .left
          .map(exc =>
            s"Problem parsing body: ${req.responseText}\n${exc.getMessage}"
          )
      )
      .recover(err =>
        Some(Left(s"Problem getting Dmn Configs: ${err.getMessage} "))
      )

  end getConfigs

  def runTests(
      configs: Seq[DmnConfig]
  ): EventStream[Either[String, Seq[Either[EvalException, DmnEvalResult]]]] =
    AjaxEventStream
      .post(s"$url/api/runDmnTests", data = configs.asJson.toString)
      .map(req =>
        parser
          .parse(req.responseText)
          .flatMap(_.as[Seq[Either[EvalException, DmnEvalResult]]])
          .left
          .map(exc =>
            s"Problem parsing response body: ${req.responseText}\n${exc.getMessage()}"
          )
      )
      .recover(err =>
        Some(Left(s"Problem running Dmn Tests: ${err.getMessage()} "))
      )

  end runTests

}
