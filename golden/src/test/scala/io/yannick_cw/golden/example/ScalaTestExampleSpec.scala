package io.yannick_cw.golden.example

import io.yannick_cw.golden.ChangeDetector
import io.yannick_cw.golden.example.UserDataInstances.{arbitrary, bsonCodec}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.scalacheck.Checkers.check

class ScalaTestExampleSpec extends AnyFunSuite with Matchers {
  test("UserData bson") {
    check(ChangeDetector[UserData](20).prop())
  }
}
