package io.golden.any

import cats.implicits.{catsStdInstancesForTry, toFunctorOps}
import io.golden.any.ChangeDetector.Codec
import io.golden.any.difflib.{AnsiColors, Diffs, Printers}
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
    s"If you want to accept the changes please delete ${AnsiColors.LightRed}${fileResource.fileName}${AnsiColors.Reset} and rerun the tests."

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

  private def checkPropWriting(expectedString: String, actualString: String): Prop =
    checkProp((exp, act) =>
      s"${AnsiColors.LightGreen}I expected to write:${AnsiColors.CYAN}\n$exp\n" +
        s"${AnsiColors.RED}But I actually would write:\n$act\n\n${AnsiColors.Reset}"
    )(expectedString, actualString)

  private def checkPropReading(expectedA: A, actualA: A): Prop =
    checkProp((exp, act) =>
      s"${AnsiColors.LightGreen}I can still read the old format:${AnsiColors.CYAN}\n$exp\n" +
        s"${AnsiColors.RED}But the new data looks different:\n$act\n\n${AnsiColors.Reset}"
    )(expectedA, actualA)

  private def checkProp[T](msg: (String, String) => String)(expected: T, actual: T): Prop =
    if (expected == actual) Prop.proved
    else {
      val exp = Printers.print(expected)
      val act = Printers.print(actual)
      Prop.falsified :| msg(exp, act) ++
        Diffs.createDiffOnlyReport(exp, act) :| resetMessage
    }

  def prop(): Prop = {
    val allProps = parseFromFile.get.flatMap {
      case (generatedA, fromFileString) =>
        List(
          codec
            .read(fromFileString)
            .fold(
              err => reportParsingError(fromFileString, err) :| resetMessage,
              fromFileA => checkPropReading(expectedA = fromFileA, actualA = generatedA)
            ),
          checkPropWriting(expectedString = fromFileString, actualString = codec.write(generatedA))
        )
    }

    Prop.all(allProps: _*)
  }

  private def reportParsingError(fromFileString: String, err: ChangeDetector.ReadErr) =
    Prop.falsified :|
      s"${AnsiColors.LightRed}Failed reading ${AnsiColors.Bold}\n${Printers.print(fromFileString)}\n${AnsiColors.Reset}" ++
        s" to ${AnsiColors.GREEN}$objectName${AnsiColors.Reset} maybe there is a new required field or a field name changed?" :|
      (s"${AnsiColors.RED}${err.msg}${AnsiColors.Reset}")
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

  // TODO add size parameter

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
