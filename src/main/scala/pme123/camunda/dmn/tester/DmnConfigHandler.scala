package pme123.camunda.dmn.tester

import java.nio.file

import ammonite.ops._
import pme123.camunda.dmn.tester.TesterValue._
import ujson._

import scala.collection.mutable.ArrayBuffer
import scala.language.implicitConversions

object DmnConfigHandler extends App {

  def read(configPath: Seq[String]): DmnConfig =
    read((pwd / configPath).toNIO)
    
  def read(configPath: file.Path): DmnConfig =
    ujson.read(configPath) match {
      case Obj(o) =>
        DmnConfig(
          o("decisionId").str,
          testerData(o("data")),
          o("dmnPath") match {
            case Arr(list) =>
              list.collect { case Str(s) => s }.toList
            case other =>
              throw new IllegalArgumentException(
                s"Not expected Json Value: $other"
              )
          }
        )
      case other =>
        throw new IllegalArgumentException(s"Not expected Json Value: $other")
    }

  def testerData(data: Value) = data match {
    case ujson.Arr(list) =>
      TesterData(
        list.collect { case Obj(m1) =>
          TesterInput(
            m1("key").str,
            m1("values") match {
              case Arr(m2) =>
                extractValues(m2)
              case other =>
                throw new IllegalArgumentException(
                  s"Not expected Json Value: $other"
                )
            }
          )
        }.toList
      )
    case other =>
      throw new IllegalArgumentException(s"Not expected Json Value: $other")

  }

  private def extractValues(m2: ArrayBuffer[Value]): List[TesterValue] = {
    m2.map {
      case Str(v)  => StringValue(v)
      case Num(v)  => NumberValue(BigDecimal(v))
      case Bool(v) => BooleanValue(v)
      case Arr(m3) => ValueSet(extractValues(m3).toSet)
      case Obj(o) if o("type").str == "RandomInts" =>
        RandomInts(o("count").num.toInt)
      case other => StringValue(other.toString)
    }.toList
  }
}

