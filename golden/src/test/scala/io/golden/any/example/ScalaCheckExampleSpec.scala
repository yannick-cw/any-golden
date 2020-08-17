package io.golden.any.example

import UserDataInstances.{arbitrary, jsonCodec}
import io.golden.any.ChangeDetector
import org.scalacheck.Properties

object ScalaCheckExampleSpec extends Properties("Models") {
  property("UserData json") = ChangeDetector[UserData](100).prop()
}
