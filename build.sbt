import sbt.Keys.resolvers

ThisBuild / version := "0.1"

ThisBuild / scalaVersion := "2.12.2"

val sparkVersion = "2.4.0"

// ========
// PROJECTS
// ========

lazy val core = project
    .settings(
      name := "spark-experiments-core",
      commonSettings,
      libraryDependencies ++= commonDependencies
    )

lazy val ml = project
    .settings(
      name := "spark-experiments-ml",
      commonSettings,
      libraryDependencies ++= commonDependencies ++ Seq(
        "org.apache.spark" %% "spark-mllib" % sparkVersion % "provided"
      )
    ).dependsOn(core)

lazy val streaming = project
    .settings(
      name := "spark-experiments-streaming",
      commonSettings,
      libraryDependencies ++= commonDependencies
    ).dependsOn(core)

lazy val demo = project
    .settings(
      name := "spark-experiments-demo",
      commonSettings,
      libraryDependencies ++= commonDependencies
    ).dependsOn(core)

// ========
// SETTINGS
// ========

resolvers += Resolver.sonatypeRepo("releases")
addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.9")

lazy val commonDependencies = Seq(
  "org.apache.spark" %% "spark-core" % sparkVersion % "provided",
  "org.apache.spark" %% "spark-sql" % sparkVersion % "provided"
)

lazy val commonSettings = Seq(
  scalacOptions += "-Ypartial-unification"
)
