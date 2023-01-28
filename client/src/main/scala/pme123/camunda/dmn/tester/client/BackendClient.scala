package pme123.camunda.dmn.tester.client

import com.raquo.airstream.core.EventStream
import com.raquo.airstream.web.AjaxEventStream
import com.raquo.airstream.web.AjaxEventStream.AjaxStreamError
import com.raquo.laminar.api.L.*
import io.circe.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.scalajs.*
import org.scalajs.dom.XMLHttpRequest
import pme123.camunda.dmn.tester.shared.*
import pme123.camunda.dmn.tester.shared.HandledTesterException.EvalException

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import scala.scalajs.js.typedarray.{ArrayBuffer, TypedArrayBuffer}

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
    AjaxEventStream
      .put(
        s"$url/api/dmnConfig?configPath=${URLEncoder.encode(path, StandardCharsets.UTF_8)}",
        data = dmnConfig.asJson.toString
      )
      .map(extractDmnConfigs)
      .recover(err =>
        Some(Left(s"Problem updating Dmn Config: ${err.getMessage} "))
      )

  // delete DmnConfig
  def deleteConfig(
      dmnConfig: DmnConfig,
      path: String
  ): EventStream[Seq[DmnConfig]] =
    AjaxEventStream
      .delete(
        s"$url/api/dmnConfig?configPath=${URLEncoder.encode(path, StandardCharsets.UTF_8)}",
        data = dmnConfig.asJson.toString
      )
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
      .map(req =>
        parser
          .parse(req.responseText)
          .flatMap(_.as[String])
          .left
          .map(exc =>
            s"Problem parsing body: ${req.responseText}\n${exc.getMessage}"
          )
      )
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
      .map(extractDmnConfigs)
      .recover(err =>
        err.printStackTrace()
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
            s"Problem parsing response body: ${req.responseText}\n${exc.getMessage}"
          )
      )
      .recover(err =>
        Some(Left(s"Problem running Dmn Tests: ${err.getMessage} "))
      )

  end runTests

  private def extractDmnConfigs(req: XMLHttpRequest) =
    parser
      .parse(req.responseText)
      .flatMap(_.as[Seq[DmnConfig]])
      .left
      .map(exc =>
        s"Problem parsing body: ${req.responseText}\n${exc.getMessage}"
      )
}
