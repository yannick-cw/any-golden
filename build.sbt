inThisBuild(
  List(
    organization := "io.yannick_cw",
    homepage := Some(url("https://github.com/yannick-cw/any-golden")),
    licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    developers := List(
      Developer(
        "yannick-cw",
        "Yannick Gladow",
        "yannick.gladow@gmail.com",
        url("https://www.dev-log.me")
      )
    )
  )
)

val root = project.in(file(".")).aggregate(`any-golden`)

lazy val `any-golden` = project
  .in(file("golden"))
  .settings(
    moduleName := "any-golden",
    libraryDependencies ++= Seq(
      "org.typelevel"         %% "cats-core"       % "2.1.1",
      "org.scalacheck"        %% "scalacheck"      % "1.14.3",
      "io.circe"              %% "circe-core"      % "0.13.0" % Test,
      "io.circe"              %% "circe-parser"    % "0.13.0" % Test,
      "io.circe"              %% "circe-generic"   % "0.13.0" % Test,
      "io.circe"              %% "circe-bson"      % "0.4.0" % Test,
      "org.scalatest"         %% "scalatest"       % "3.2.1" % Test,
      "org.scalatestplus"     %% "scalacheck-1-14" % "3.2.1.0" % Test,
      scalaOrganization.value % "scala-reflect"    % scalaVersion.value % Provided
    )
  )
