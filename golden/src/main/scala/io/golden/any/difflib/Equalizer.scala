/*
  This class is taken from https://github.com/scalameta/munit
  according to the original Apache 2.0 licence
 */
package io.golden.any.difflib

trait Equalizer[T] {
  def equals(original: T, revised: T): Boolean
}
object Equalizer {
  def default[T]: Equalizer[T] = new Equalizer[T] {
    override def equals(original: T, revised: T): Boolean =
      original.equals(revised)
  }
}
