/*
  This class is taken from https://github.com/scalameta/munit
  according to the original Apache 2.0 licence
  In addition the code was modified by me
 */
package io.golden.any.difflib

object Diffs {

  def create(obtained: String, expected: String): Diff =
    new Diff(obtained, expected)

  def createDiffOnlyReport(
      obtained: String,
      expected: String
  ): String =
    create(obtained, expected).createDiffOnlyReport()
}
