package io.golden.any.example

import UserDataInstances.{arbitrary, bsonCodec}
import io.golden.any.ChangeDetector
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.scalacheck.Checkers.check

class ScalaTestExampleSpec extends AnyFunSuite with Matchers {
  test("UserData bson") {
    check(ChangeDetector[UserData](20).prop())
  }
}
