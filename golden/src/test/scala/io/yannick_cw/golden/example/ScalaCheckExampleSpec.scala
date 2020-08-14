package io.yannick_cw.golden.example

import io.yannick_cw.golden.ChangeDetector
import io.yannick_cw.golden.example.UserDataInstances.{arbitrary, jsonCodec}
import org.scalacheck.Properties

object ScalaCheckExampleSpec extends Properties("Models") {
  property("UserData json") = ChangeDetector[UserData](100).prop()
}
