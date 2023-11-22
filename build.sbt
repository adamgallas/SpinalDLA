name := "SpinalDLA"

version := "0.1"

scalaVersion := "2.11.12"

organization := "casia"

val spinalVersion = "1.9.0"

libraryDependencies ++= Seq(
  "com.github.spinalhdl" % "spinalhdl-core_2.11" % spinalVersion,
  "com.github.spinalhdl" % "spinalhdl-lib_2.11" % spinalVersion,
  compilerPlugin("com.github.spinalhdl" % "spinalhdl-idsl-plugin_2.11" % spinalVersion)
)

githubOwner := "adamgallas"
githubRepository := "SpinalDLA"
githubTokenSource := TokenSource.GitConfig("github.token")