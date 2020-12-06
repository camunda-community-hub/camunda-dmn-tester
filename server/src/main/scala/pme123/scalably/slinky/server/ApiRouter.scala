package pme123.scalably.slinky.server

import java.nio.ByteBuffer

import boopickle.Default.{Pickle, Pickler}
import boopickle.UnpickleImpl

// Autowire / Boopickle automatic Serialization
object ApiRouter extends autowire.Server[ByteBuffer, Pickler, Pickler] {

  // Unpickle was not correct in Intellij > UnpickleImpl
  override def read[R: Pickler](p: ByteBuffer): R = UnpickleImpl[R].fromBytes(p)

  override def write[R: Pickler](r: R): ByteBuffer = Pickle.intoBytes(r)
}
