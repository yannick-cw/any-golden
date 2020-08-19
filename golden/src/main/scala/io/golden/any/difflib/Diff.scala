/*
  This class is taken from https://github.com/scalameta/munit
  according to the original Apache 2.0 licence
  In addition the code was modified by me
 */
package io.golden.any.difflib

import scala.collection.JavaConverters._

object AnsiColors {
  val LightRed   = "\u001b[91m"
  val LightGreen = "\u001b[92m"
  val Reset      = "\u001b[0m"
  val Reversed   = "\u001b[7m"
  val Bold       = "\u001b[1m"
  val Faint      = "\u001b[2m"
  val RED        = "\u001B[31m"
  val YELLOW     = "\u001B[33m"
  val BLUE       = "\u001B[34m"
  val Magenta    = "\u001B[35m"
  val CYAN       = "\u001B[36m"
  val GREEN      = "\u001B[32m"
  val DarkGrey   = "\u001B[90m"

  def c(s: String, colorSequence: String): String =
    if (colorSequence == null) s
    else colorSequence + s + Reset

  def filterAnsi(s: String): String =
    if (s == null) {
      null
    } else {
      val len = s.length
      val r   = new java.lang.StringBuilder(len)
      var i   = 0
      while (i < len) {
        val c = s.charAt(i)
        if (c == '\u001B') {
          i += 1
          while (i < len && s.charAt(i) != 'm') i += 1
        } else {
          r.append(c)
        }
        i += 1
      }
      r.toString()
    }

}
class Diff(val obtained: String, val expected: String) extends Serializable {
  val obtainedClean: String      = AnsiColors.filterAnsi(obtained)
  val expectedClean: String      = AnsiColors.filterAnsi(expected)
  val obtainedLines: Seq[String] = splitIntoLines(obtainedClean)
  val expectedLines: Seq[String] = splitIntoLines(expectedClean)
  val unifiedDiff: String        = createUnifiedDiff(obtainedLines, expectedLines)
  def isEmpty: Boolean           = unifiedDiff.isEmpty()

  def createReport(
      title: String,
      printObtainedAsStripMargin: Boolean = true
  ): String = {
    val sb = new StringBuilder
    if (title.nonEmpty) {
      sb.append(title)
        .append("\n")
    }
    if (obtainedClean.length < 1000) {
      header("Obtained", sb).append("\n")
      if (printObtainedAsStripMargin) {
        sb.append(asStripMargin(obtainedClean))
      } else {
        sb.append(obtainedClean)
      }
      sb.append("\n")
    }
    appendDiffOnlyReport(sb)
    sb.toString()
  }

  def createDiffOnlyReport(): String = {
    val out = new StringBuilder
    appendDiffOnlyReport(out)
    out.toString()
  }

  private def appendDiffOnlyReport(sb: StringBuilder): Unit = {
    header("Diff", sb)
    sb.append(
        s" (${AnsiColors.LightRed}- obtained${AnsiColors.Reset}, ${AnsiColors.LightGreen}+ expected${AnsiColors.Reset})"
      )
      .append("\n")
    sb.append(unifiedDiff)
    ()
  }

  private def asStripMargin(obtained: String): String =
    if (!obtained.contains("\n")) obtained
    else {
      val out   = new StringBuilder
      val lines = obtained.trim.linesIterator
      out.append("    \"\"\"|" + lines.next() + "\n")
      lines.foreach(line => out.append("       |").append(line).append("\n"))
      out.append("       |\"\"\".stripMargin")
      out.toString()
    }

  private def header(t: String, sb: StringBuilder): StringBuilder =
    sb.append(AnsiColors.c(s"=> $t", AnsiColors.Bold))

  private def createUnifiedDiff(
      original: Seq[String],
      revised: Seq[String]
  ): String = {
    val diff = DiffUtils.diff(original.asJava, revised.asJava)
    val result =
      if (diff.getDeltas.isEmpty) ""
      else {
        DiffUtils
          .generateUnifiedDiff(
            "obtained",
            "expected",
            original.asJava,
            diff,
            1
          )
          .asScala
          .iterator
          .drop(2)
          .filterNot(_.startsWith("@@"))
          .map { line =>
            if (line.isEmpty()) line
            else if (line.last == ' ') line + "∙"
            else line
          }
          .map { line =>
            if (line.startsWith("-")) AnsiColors.c(line, AnsiColors.LightRed)
            else if (line.startsWith("+"))
              AnsiColors.c(line, AnsiColors.LightGreen)
            else line
          }
          .mkString("\n")
      }
    result
  }

  private def splitIntoLines(string: String): Seq[String] =
    string.trim().replaceAllLiterally("\r\n", "\n").split("\n").toIndexedSeq
}
