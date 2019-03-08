name := "spark-experiments"

version := "0.1"

scalaVersion := "2.12.2"

val sparkVersion = "2.4.0"

scalacOptions += "-Ypartial-unification"

resolvers += Resolver.sonatypeRepo("releases")

addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.9")

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % sparkVersion,
  "org.apache.spark" %% "spark-sql" % sparkVersion,
  "org.apache.spark" %% "spark-mllib" % sparkVersion,
  "org.typelevel" %% "cats-core" % "1.6.0"
)