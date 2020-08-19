/*
  This class is taken from https://github.com/scalameta/munit
  according to the original Apache 2.0 licence
 */
package io.golden.any.difflib

import java.util

trait DiffAlgorithm[T] {
  def diff(original: util.List[T], revised: util.List[T]): Patch[T]
}
