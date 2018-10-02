name in ThisBuild := "youi-template"
organization in ThisBuild := "io.youi"
version in ThisBuild := "1.0.0-SNAPSHOT"
scalaVersion in ThisBuild := "2.12.6"

val youi = "0.9.0-M20"
val powerScala = "2.0.5"

lazy val template = crossApplication.in(file("."))
  .settings(
    youiVersion := youi,
    name := "youi-template"
  )
  .jvmSettings(
    fork := true,
    libraryDependencies ++= Seq(
      "io.youi" %% "youi-optimizer" % youi,
      "org.powerscala" %% "powerscala-io" % powerScala
    ),
    assemblyJarName in assembly := "youi-template.jar"
  )
lazy val templateJS = template.js
lazy val templateJVM = template.jvm