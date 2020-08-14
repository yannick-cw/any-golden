package io.yannick_cw.golden.example

import java.util.UUID

import io.circe.generic.auto._
import io.yannick_cw.golden.ChangeDetector.{Codec, ReadErr}
import org.scalacheck.{Arbitrary, Gen}

case class UserData(id: UUID, name: String, age: Option[Int])

object UserDataInstances {
  implicit val arbitrary: Arbitrary[UserData] = Arbitrary(
    for {
      id   <- Arbitrary.arbitrary[UUID]
      name <- Gen.alphaStr
      age  <- Arbitrary.arbitrary[Option[Int]]
    } yield UserData(id, name, age)
  )

  // Using Circe as json codec
  import io.circe.bson._
  import io.circe.parser.parse
  import io.circe.syntax._

  // bson is checked by round tripping from json to bson to json to writing a string to a file
  implicit val bsonCodec: Codec[UserData] = new Codec[UserData] {
    override val codecFor: String = "bson"
    override def write(a: UserData): String =
      jsonBsonReader.read(jsonBsonWriter.write(a.asJson)).spaces4
    override def read(s: String): Either[ReadErr, UserData] =
      parse(s).left
        .map(f => ReadErr(f.message))
        .map(jsonBsonWriter.write)
        .map(jsonBsonReader.read)
        .flatMap(_.as[UserData].left.map(fail => ReadErr(fail.getMessage())))
  }

  implicit val jsonCodec: Codec[UserData] = new Codec[UserData] {
    override val codecFor: String           = "json"
    override def write(a: UserData): String = a.asJson.spaces4
    override def read(s: String): Either[ReadErr, UserData] =
      for {
        json <- parse(s).left.map(f => ReadErr(f.message))
        v    <- json.as[UserData].left.map(f => ReadErr(f.getMessage()))
      } yield v
  }
}
