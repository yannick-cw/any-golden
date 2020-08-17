package io.golden.any

import cats.implicits.{catsStdInstancesForTry, toFunctorOps}
import ChangeDetector.Codec
import org.scalacheck.util.Pretty
import org.scalacheck.{Arbitrary, Prop}

import scala.reflect.runtime.universe.TypeTag
import scala.util.{Success, Try}

class ChangeDetector[A](
    fileResource: CodecFileResource[A],
    exampleGen: ExampleGeneration[A],
    codec: Codec[A],
    numberTests: Int,
    objectName: String
) {

  private def resetMessage =
    s"If you want to accept the changes please delete ${fileResource.fileName} and rerun the tests."

  private def parseFromFile: Try[List[(A, String)]] =
    fileResource.readFromFiles.flatMap {
      case Nil => generateFile()
      case results =>
        Success(results.map(rr => (exampleGen.getValueFromBase64Seed(rr.seed), rr.value)))
    }

  private def generateFile(): Try[List[(A, String)]] = {
    val examples = exampleGen.generateRandomGoldenExamples(numberTests)

  fileResource
      .writeToFiles(examples.map { case (seed, _, s) => (s, seed) })
      .as(examples.map { case (_, a, s) => (a, s) })
  }

  private def checkPropWriting(x: String, y: String): Prop =
    if (x == y) Prop.proved
    else
      Prop.falsified :| {
        val exp = Pretty.pretty[Any](y, Pretty.Params(0))
        val act = Pretty.pretty[Any](x, Pretty.Params(0))
        s"I expected to write: \n$exp\n\n" + s"But I actually would write: $act."
      } :| resetMessage

  private def checkPropReading(x: A, y: A): Prop =
    if (x == y) Prop.proved
    else
      Prop.falsified :| {
        val exp = Pretty.pretty(x, Pretty.Params(0))
        val act = Pretty.pretty(y, Pretty.Params(0))
        s"I can still read the old format: \n$exp\n\n" + s"But the new data looks different: $act"
      } :| resetMessage

  def prop(): Prop = {
    // TODO get not cool - or ok?
    val allProps = parseFromFile.get.flatMap {
      case (a, s) =>
        List(
          codec
            .read(s)
            .fold(
              err =>
                Prop.falsified :| s"Failed reading \n$s\n to $objectName maybe there is a new required field or a field name changed?" :| (err.msg) :| resetMessage,
              checkPropReading(_, a)
            ),
          checkPropWriting(codec.write(a), s)
        )
    }

    Prop.all(allProps: _*)
  }
}

object ChangeDetector {

  case class ReadErr(msg: String)
  trait Codec[A] {
    // used to identify in which file to save and read, e.g. json, bson, yaml
    val codecFor: String
    def write(a: A): String
    def read(s: String): Either[ReadErr, A]
  }

  /**
    * Runs and creates exactly one golden test
    */
  def apply[A: TypeTag](implicit codec: Codec[A], arb: Arbitrary[A]): ChangeDetector[A] =
    apply(1)

  /**
    * @param numberTests How many golden tests to create for A
    */
  def apply[A: TypeTag](
      numberTests: Int
  )(implicit codec: Codec[A], arb: Arbitrary[A]): ChangeDetector[A] =
    new ChangeDetector[A](
      FileResource(
        Resources.inferName,
        Resources.inferRootDir,
        Resources.inferPackage,
        codec.codecFor
      ),
      new ExampleGeneration[A](codec, arb.arbitrary),
      codec,
      numberTests,
      Resources.inferName
    )
}
