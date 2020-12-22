package pme123.camunda.dmn.tester.server.config

import java.io.File

object DmnUploader {

  def upload(dmn: File) = {
    println(s"DMN FILE: ${dmn.getAbsolutePath}")
  }

}
