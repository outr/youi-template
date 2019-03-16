name in ThisBuild := "youi-template"
organization in ThisBuild := "io.youi"
version in ThisBuild := "1.0.0"
scalaVersion in ThisBuild := "2.12.8"
resolvers in ThisBuild += Resolver.sonatypeRepo("releases")

publishTo in ThisBuild := sonatypePublishTo.value
sonatypeProfileName in ThisBuild := "io.youi"
publishMavenStyle in ThisBuild := true
licenses in ThisBuild := Seq("MIT" -> url("https://github.com/outr/youi-template/blob/master/LICENSE"))
sonatypeProjectHosting in ThisBuild := Some(xerial.sbt.Sonatype.GitHubHosting("outr", "youi-template", "matt@outr.com"))
homepage in ThisBuild := Some(url("https://github.com/outr/youi-template"))
scmInfo in ThisBuild := Some(
  ScmInfo(
    url("https://github.com/outr/youi-template"),
    "scm:git@github.com:outr/youi-template.git"
  )
)
developers in ThisBuild := List(
  Developer(id="darkfrog", name="Matt Hicks", email="matt@matthicks.com", url=url("http://matthicks.com"))
)

val youi = "0.10.9"
val powerScala = "2.0.5"
val jsass = "5.7.3"

lazy val template = crossApplication.in(file("."))
  .settings(
    youiVersion := youi,
    name := "youi-template"
  )
  .jvmSettings(
    fork := true,
    libraryDependencies ++= Seq(
      "io.youi" %% "youi-optimizer" % youi,
      "org.powerscala" %% "powerscala-io" % powerScala,
      "io.bit3" % "jsass" % jsass
    ),
    assemblyJarName in assembly := "youi-template.jar"
  )
lazy val templateJS = template.js
lazy val templateJVM = template.jvm
