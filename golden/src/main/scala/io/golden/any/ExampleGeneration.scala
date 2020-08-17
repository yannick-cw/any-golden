package io.golden.any

import ChangeDetector.Codec
import org.scalacheck.Gen
import org.scalacheck.rng.Seed

trait ExampleGen[A] {
  def getValueFromBase64Seed(seed: Seed): A
  def generateRandomGoldenExamples(nrTests: Int): List[(Seed, A, String)]
}

class ExampleGeneration[A](codec: Codec[A], gen: Gen[A]) extends ExampleGen[A] {
  private def getValue(seed: Seed): A = gen.pureApply(Gen.Parameters.default, seed)

  final override def getValueFromBase64Seed(seed: Seed): A = getValue(seed)

  final override def generateRandomGoldenExamples(nrTests: Int): List[(Seed, A, String)] =
    0.until(nrTests)
      .map { _ =>
        val seed  = Seed.random()
        val value = getValue(seed)

        (seed, value, codec.write(value))
      }
      .toList
}
