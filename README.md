# any-golden

**ALPHA** mode

### In short

Catch (unwanted)changes to your API early and reliable, without much effort.
Ever heard about [golden testing](https://ro-che.info/articles/2017-12-04-golden-tests) or [Snapshot testing](https://jestjs.io/docs/en/snapshot-testing)
or used [circe-golden](https://github.com/circe/circe-golden), but want to use the same for other serialization/deserialization libraries?

Here you go!

### Why and how?

This is a simple library helping you to detect changes to your API. It is a common problem, that we sometimes use data models 
in our API **and** in our business logic. E.g. if we have a user type:
```Scala
case class UserData(id: UUID, name: String, age: Option[Int])
```
which we use to decode from JSON, in our Server from a client call, internally read, modify and what else and in the write
the same model to a Database.
And usually these decodings and encodings happen with the help of some marco magic, we do not write them our selves.

So we might think, okay cool, lets add some new field to our `UserData`: `email: String`. 

Maybe we even think of giving it some default value, when a client calls withouth the `email` field, but uuups we
forgot, that we can not read from our database anymore, as old Users do not have an `email`.

Even worse: our tests did not catch that and it is shipped to production now! Rollback time.

This library aims to prevent any such scenario, by informing you if your api changes, even if it is maybe not a breaking change.

Three easy steps:

1. Define an [Arbitrary[Codec]](https://github.com/typelevel/scalacheck/blob/master/doc/UserGuide.md#the-arbitrary-generator) instance
for your data type, e.g. something that can generate `UserData`.
```Scala
import org.scalacheck.{Arbitrary, Gen}

  implicit val arbitrary: Arbitrary[UserData] = Arbitrary(
    for {
      id   <- Arbitrary.arbitrary[UUID]
      name <- Gen.alphaStr
      age  <- Arbitrary.arbitrary[Option[Int]]
    } yield UserData(id, name, age)
  )
```

2. Define a `Codec[Userdata]` (here we use circe for json)
```Scala
import io.circe.generic.auto._
import io.golden.any.ChangeDetector.{Codec, ReadErr}

  implicit val jsonCodec: Codec[UserData] = new Codec[UserData] {
    override val codecFor: String           = "json"
    override def write(a: UserData): String = a.asJson.spaces4
    override def read(s: String): Either[ReadErr, UserData] =
      for {
        json <- parse(s).left.map(f => ReadErr(f.message))
        v    <- json.as[UserData].left.map(f => ReadErr(f.getMessage()))
      } yield v
  }
```

3. Run the tests in scalatest or scalacheck
```Scala
import io.golden.any.ChangeDetector
import io.golden.any.example.UserDataInstances.{arbitrary, jsonCodec}
import org.scalacheck.Properties

object ScalaCheckExampleSpec extends Properties("Models") {
  property("UserData json") = ChangeDetector[UserData](100).prop()
}
```

```Scala
import io.golden.any.ChangeDetector
import io.golden.any.example.UserDataInstances.{arbitrary, jsonCodec}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.scalacheck.Checkers.check

class ScalaTestExampleSpec extends AnyFunSuite with Matchers {
  test("UserData json") {
    check(ChangeDetector[UserData](20).prop())
  }
}
```

Thats it!

Now when you run the tests the first time it creates a number of testcases and writes them to you test resources.
These need best to be checked in into version control, so the next time you run them they can be compared.

They look something like this:
```
IppJKOZFUwthHTL_yAaCZi4IrMYZ46-mjiajmft35VE=
{
    "id" : "4579ded7-e68d-48a3-8358-11a8b9e97144",
    "name" : "aughbdodsqgtydpaWwecqbtipjbbprlmi",
    "age" : null
}
```
(you can generate as many testcases per model as you want (e.g. to catch Option being defined and None))

What's that long, cryptic line on top? The `seed` which generated this example, so next time when you compare them it can
generate the same!

Okay so let's see what happens if you change your API and add the email field:

```
  Labels of failing property:
    Failed reading 
{
    "id" : "bf30c34b-0ef7-4903-ae28-ae1eb5d3a2a5",
    "name" : "vwzorkznflimGfmcggtqhqmhydOzbcwkcgrhdfgkgUerrpuuwcUEhvkxk",
    "age" : -861516569
}
 to UserData maybe there is a new required field or a field name changed?
    Attempt to decode value on failed cursor: DownField(email)
    If you want to accept the changes please delete
 /Users/me/any-golden/golden/src/test/resources/io/golden/any/exampleUserData.json and rerun the tests.
```

Okay but what about when we add an optional `email: Option[String]` field:

```
  Labels of failing property:
    I can still read the old format: 
UserData(bf30c34b-0ef7-4903-ae28-ae1eb5d3a2a5,vwzorkznflimGfmcggtqhqmhydOzbcwkcgrhdfgkgUerrpuuwcUEhvkxk,Some(-861516569),None)
But the new data looks different:
 UserData(bf30c34b-0ef7-4903-ae28-ae1eb5d3a2a5,vwzorkznflimGfmcggtqhqmhydOzbcwkcgrhdfgkgUerrpuuwcUEhvkxk,Some(-861516569),Some(email@mail.com))
    If you want to accept the changes please delete
 /Users/me/any-golden/golden/src/test/resources/io/golden/any/exampleUserData.json and rerun the tests.
```

Well it still tells us, that our api changed!

One more case, what if we remove a field, e.g. `age`:

```
 Labels of failing property:
    I expected to write: 
{
    "id" : "bf30c34b-0ef7-4903-ae28-ae1eb5d3a2a5",
    "name" : "vwzorkznflimGfmcggtqhqmhydOzbcwkcgrhdfgkgUerrpuuwcUEhvkxk",
    "age" : -861516569
}
But I actually would write: {
    "id" : "bf30c34b-0ef7-4903-ae28-ae1eb5d3a2a5",
    "name" : "vwzorkznflimGfmcggtqhqmhydOzbcwkcgrhdfgkgUerrpuuwcUEhvkxk"
}.
    If you want to accept the changes please delete
 /Users/me/any-golden/golden/src/test/resources/io/golden/any/exampleUserData.json and rerun the tests.
```

And that's it. This should give you an easy tool to catch API changes without much effort.

### Other
This project uses source code from https://github.com/circe/circe-golden, licensed under the Apache 2.0 license.
It is doing a lot of the same as `circe-golden`, but aims to work with any library for which you can write an `Codec` instance.
