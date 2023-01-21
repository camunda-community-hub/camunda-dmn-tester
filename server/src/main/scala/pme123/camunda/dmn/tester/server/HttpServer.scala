package pme123.camunda.dmn.tester.server

import cats.effect._
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.server.blaze._
import org.http4s.server.middleware.CORS
import org.http4s.server.{Router, Server}
import org.slf4j.{Logger, LoggerFactory}
import os.pwd
import pme123.camunda.dmn.tester.shared._

import java.io.File
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import scala.concurrent.ExecutionContext.global

object HttpServer extends IOApp {

  val logger: Logger = LoggerFactory.getLogger(getClass)

  override def run(args: List[String]): IO[ExitCode] =
    IO(logger.info("Server starting at Port 8883")) *>
      app.use(_ => IO.never).as(ExitCode.Success)

  private val app: Resource[IO, Server[IO]] =
    for {
      blocker <- Blocker[IO]
      server <- BlazeServerBuilder[IO](global)
        .bindHttp(8883, "0.0.0.0")
        .withHttpApp(CORS(httpApp(blocker)))
        .resource
    } yield server

  private def httpApp(blocker: Blocker) =
    Router("/" -> guiServices(blocker), "/api" -> apiServices).orNotFound

  private object ConfigPathQueryParamMatcher
      extends QueryParamDecoderMatcher[String]("configPath")

  private lazy val apiServices = HttpRoutes.of[IO] {
    case GET -> Root / "basePath" =>
      Ok(DmnService.getBasePath())
    case GET -> Root / "configPaths" =>
      Ok(DmnService.getConfigPaths().asJson)
    case GET -> Root / "dmnConfigs" :? ConfigPathQueryParamMatcher(
          configPath
        ) =>
      val decConfigPath = URLDecoder.decode(configPath, StandardCharsets.UTF_8)
      val configs =
        if (decConfigPath.isBlank)
          Seq.empty
        else
          DmnService
            .getConfigs(decConfigPath.split("/"))
      Ok(configs.asJson)

    case req @ POST -> Root / "runDmnTests" =>
      Ok(
        req
          .as[Option[Seq[DmnConfig]]]
          .map(configs =>
            if (configs.nonEmpty && configs.get.nonEmpty) {
              val result = DmnService.runTests(configs.get).asJson
              result
            }
            else
              Seq.empty.asJson
          )
      )
    case req @ PUT -> Root / "dmnConfig" :? ConfigPathQueryParamMatcher(configPath) =>
      println(s"update DMN Config: started: ")
      val decConfigPath = URLDecoder.decode(configPath, StandardCharsets.UTF_8)
      Ok(
        req
         .as[DmnConfig]
         .map(config => {
           DmnService
             .updateConfig(config, decConfigPath.split("/"))
         }
        ).map(_.asJson)
      )
    case req @ DELETE -> Root / "dmnConfig" :? ConfigPathQueryParamMatcher(configPath) =>
      println(s"update DMN Config: started: ")
      val decConfigPath = URLDecoder.decode(configPath, StandardCharsets.UTF_8)
      Ok(
        req
          .as[DmnConfig]
          .map(config => {
            DmnService
              .deleteConfig(config, decConfigPath.split("/"))
          }
          ).map(_.asJson)
      )
  }

  private def guiServices(blocker: Blocker) = HttpRoutes.of[IO] {
    case req if req.method == Method.OPTIONS =>
      IO(Response(Ok, headers = Headers.of(Header("Allow", "OPTIONS, POST"))))
    case request if request.uri.path.startsWith("/testReports") =>
      testReports(blocker, request)
    case request @ GET -> Root =>
      static("index.html", blocker, request)
    case request @ GET -> path =>
      static(path.toString, blocker, request)
  }

  private val testReportsPath: os.Path = pwd / "target" / "test-reports"

  private def static(file: String, blocker: Blocker, request: Request[IO]) = {
    logger.info(s"Static File: $file")
    StaticFile
      .fromResource("/" + file, blocker, Some(request))
      .orElse(
        StaticFile
          .fromFile(
            new File(testReportsPath.toIO.getAbsolutePath + file),
            blocker,
            Some(request)
          )
      )
      .getOrElseF(NotFound())
  }

  private def testReports(blocker: Blocker, request: Request[IO]) = {
    logger.info(s"TestReports path: $testReportsPath")
    StaticFile
      .fromFile((testReportsPath / "index.html").toIO, blocker, Some(request))
      .getOrElseF(NotFound())
  }

  implicit lazy val decoderConfig: EntityDecoder[IO, DmnConfig] = jsonOf[IO, DmnConfig]
  implicit lazy val decoderOptConfigs: EntityDecoder[IO, Option[Seq[DmnConfig]]] = jsonOf[IO, Option[Seq[DmnConfig]]]
  implicit lazy val decoderConfigs: EntityDecoder[IO, Seq[DmnConfig]] = jsonOf[IO, Seq[DmnConfig]]

}
