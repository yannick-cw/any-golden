package io.yannick_cw.golden

import java.io.{File, PrintWriter}

import cats.instances.list._
import cats.instances.try_._
import cats.syntax.traverse._
import org.scalacheck.rng.Seed

import scala.util.Try

case class ReadResult(value: String, seed: Seed)
trait CodecFileResource[A] {
  def fileName: String
  def readFromFiles: Try[List[ReadResult]]
  def writeToFiles(toWrite: List[(String, Seed)]): Try[Unit]
}

class FileResource[A](
    name: String,
    resourceRootDir: File,
    resourcePackage: List[String],
    codecName: String
) extends CodecFileResource[A] {

  override def fileName: String = resourceDir.getCanonicalPath ++ nameMatcher

  private val nameMatcher = name ++ "." ++ codecName
  private val separator   = "------------------------------"

  private val resourceRootPath: String = "/" + resourcePackage.mkString("/") + "/"
  private val resourceDir: File = resourcePackage.foldLeft(resourceRootDir) {
    case (acc, p) => new File(acc, p)
  }

  override def writeToFiles(toWrite: List[(String, Seed)]): Try[Unit] =
    Try {
      resourceDir.mkdirs()
      val file = new File(resourceDir, s"$name.$codecName")
      val formattedForWriting = toWrite
        .map { case (content, seed) => seed.toBase64 ++ "\n" ++ content }
        .mkString("\n" ++ separator ++ "\n")

      val writer = new PrintWriter(file)
      writer.print(formattedForWriting)
      writer.close()
    }

  override def readFromFiles: Try[List[ReadResult]] =
    Resources
      .open(resourceRootPath)
      .flatMap { dirSource =>
        val files = dirSource.getLines
          .find {
            case `nameMatcher` => true
            case _             => false
          }
          .toList
          .flatTraverse { name =>
            val content = Resources.open(resourceRootPath + name).map { source =>
              val lines =
                source.getLines.mkString("\n").split(separator ++ "\n").toList.map { line =>
                  val all     = line.split('\n').toList
                  val seed    = all.head
                  val content = all.tail

                  (content.mkString("\n"), seed)
                }
              source.close()
              lines
            }

            for {
              c <- content
              r <- c.traverse { case (con, seed) => Seed.fromBase64(seed).map(ReadResult(con, _)) }
            } yield r
          }

        dirSource.close()

        files
      }
}

object FileResource {
  def apply[A](
      name: String,
      resourceRootDir: File,
      resourcePackage: List[String],
      codecName: String
  ): CodecFileResource[A] = new FileResource[A](name, resourceRootDir, resourcePackage, codecName)

}
