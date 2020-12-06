package pme123.scalably.slinky.services

import java.nio.ByteBuffer

import boopickle.Default._
import boopickle.UnpickleImpl
import org.scalajs.dom.ext.Ajax
import org.scalajs.dom.window

import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.js.typedarray.{ArrayBuffer, TypedArrayBuffer}

object AjaxClient extends autowire.Client[ByteBuffer, Pickler, Pickler] {
  implicit val ec: ExecutionContext = ExecutionContext.global

  private lazy val url =
    if (window.location.port == "8024") {
      println("ATTENTION: CORS - see https://stackoverflow.com/a/38000615/2750966")
      "http://localhost:8883"
    }
    else ""

  override def doCall(req: Request): Future[ByteBuffer] = {
    println(s"API URL: $url")
    Ajax.post(
      url = s"$url/api/" + req.path.mkString("/"),
      data = Pickle.intoBytes(req.args),
      responseType = "arraybuffer",
      headers = Map("Content-Type" -> "application/octet-stream")
    ).map(r => TypedArrayBuffer.wrap(r.response.asInstanceOf[ArrayBuffer]))
  }

  override def read[Result: Pickler](p: ByteBuffer): Result = UnpickleImpl[Result].fromBytes(p)

  override def write[Result: Pickler](r: Result): ByteBuffer = Pickle.intoBytes(r)
}
