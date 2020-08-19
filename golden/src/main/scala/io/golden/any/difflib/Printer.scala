/*
  This class is taken from https://github.com/scalameta/munit
  according to the original Apache 2.0 licence
  In addition the code was modified by me
 */
package io.golden.any.difflib

import scala.annotation.switch

trait Printer {

  /**
    * Pretty-print a single value during pretty printing.
    *
    * Returns true if this value has been printed, false if FunSuite should fallback to the default pretty-printer.
    */
  def print(value: Any, out: StringBuilder, indent: Int): Boolean
  def height: Int = 100
  def isMultiline(string: String): Boolean =
    string.contains('\n')
}

/** Default printer that does not customize the pretty-printer */
object EmptyPrinter extends Printer {
  def print(value: Any, out: StringBuilder, indent: Int): Boolean = false
}

trait Printable {
  def print(out: StringBuilder, indent: Int): Unit
}

object Compat {
  type LazyList[+T] = Stream[T]
  val LazyList = scala.Stream
  def productElementNames(): Iterator[String] =
    Iterator.continually("")
  def collectionClassName(i: Iterable[_]): String =
    i.asInstanceOf[{ def collectionClassName: String }].collectionClassName
}

object Printers {

  /** Pretty-prints the value in a format that's optimized for producing diffs */
  def print(any: Any, printer: Printer = EmptyPrinter): String = {
    var height     = printer.height
    val out        = new StringBuilder()
    val indentStep = 2
    def loop(a: Any, indent: Int): Unit = {
      height -= 1
      if (height < 0) {
        out.append("...")
        return
      }
      val nextIndent = indent + indentStep
      val isDone     = printer.print(a, out, indent)
      if (!isDone) {
        a match {
          case null         => out.append("null"); ()
          case x: Printable => x.print(out, indent)
          case x: Char =>
            out.append('\'')
            printChar(x, out)
            out.append('\''); ()
          case x: Byte   => out.append(x.toString()); ()
          case x: Short  => out.append(x.toString()); ()
          case x: Int    => out.append(x.toString()); ()
          case x: Long   => out.append(x.toString()); ()
          case x: Float  => out.append(x.toString()); ()
          case x: Double => out.append(x.toString()); ()
          case x: String => printString(x, out, printer)
          case None =>
            out.append("None"); ()
          case Nil =>
            out.append("Nil"); ()
          case x: Map[_, _] =>
            printApply(
              Compat.collectionClassName(x),
              x.iterator,
              out,
              indent,
              nextIndent
            ) {
              case (key, value) =>
                loop(key, nextIndent)
                out.append(" -> ")
                loop(value, nextIndent)
            }
          case x: Iterable[_] =>
            printApply(
              Compat.collectionClassName(x),
              x.iterator,
              out,
              indent,
              nextIndent
            )(value => loop(value, nextIndent))
          case x: Array[_] =>
            printApply(
              "Array",
              x.iterator,
              out,
              indent,
              nextIndent
            )(value => loop(value, nextIndent))
          case it: Iterator[_] =>
            if (it.isEmpty) { out.append("empty iterator"); () }
            else { out.append("non-empty iterator"); () }
          case p: Product =>
            val elementNames = Compat.productElementNames()
            val infiniteElementNames = Iterator.continually {
              if (elementNames.hasNext) elementNames.next()
              else ""
            }
            printApply(
              p.productPrefix,
              p.productIterator.zip(infiniteElementNames),
              out,
              indent,
              nextIndent
            ) {
              case (value, key) =>
                if (key.nonEmpty) {
                  out.append(key).append(" = ")
                }
                loop(value, nextIndent)
            }
          case _ =>
            out.append(a.toString()); ()
        }
      }
    }
    loop(any, indent = 0)
    AnsiColors.filterAnsi(out.toString())
  }

  private def printApply[T](
      prefix: String,
      it: Iterator[T],
      out: StringBuilder,
      indent: Int,
      nextIndent: Int,
      open: String = "(",
      close: String = ")",
      comma: String = ","
  )(fn: T => Unit): Unit = {
    out.append(prefix)
    out.append(open)
    if (it.hasNext) {
      printNewline(out, nextIndent)
      while (it.hasNext) {
        val value = it.next()
        fn(value)
        if (it.hasNext) {
          out.append(comma)
          printNewline(out, nextIndent)
        } else {
          printNewline(out, indent)
        }
      }
    }
    out.append(close); ()
  }

  private def printNewline(out: StringBuilder, indent: Int): Unit = {
    out.append("\n")
    var i = 0
    while (i < indent) {
      out.append(' ')
      i += 1
    }
  }

  private def printString(
      string: String,
      out: StringBuilder,
      printer: Printer
  ): Unit = {
    val isMultiline = printer.isMultiline(string)
    if (isMultiline) {
      out.append('"')
      out.append('"')
      out.append('"')
      out.append(string)
      out.append('"')
      out.append('"')
      out.append('"'); ()
    } else {
      out.append('"')
      var i = 0
      while (i < string.length()) {
        printChar(string.charAt(i), out)
        i += 1
      }
      out.append('"'); ()
    }
  }

  private def printChar(c: Char, sb: StringBuilder) =
    (c: @switch) match {
      case '"'  => sb.append("\\\"")
      case '\'' => sb.append("\\'")
      case '\\' => sb.append("\\\\")
      case '\b' => sb.append("\\b")
      case '\f' => sb.append("\\f")
      case '\n' => sb.append("\\n")
      case '\r' => sb.append("\\r")
      case '\t' => sb.append("\\t")
      case c =>
        val isUnicode = false
        if (c < ' ' || (c > '~' && isUnicode))
          sb.append("\\u%04x" format c.toInt)
        else sb.append(c)
    }

}
