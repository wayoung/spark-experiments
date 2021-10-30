import sbt.Keys.resolvers

ThisBuild / version := "0.1"

ThisBuild / scalaVersion := "2.12.15"

val sparkVersion = "3.2.0"

// ========
// PROJECTS
// ========

lazy val core = project
    .settings(
      name := "spark-experiments-core",
      commonSettings,
      libraryDependencies ++= commonDependencies
    )

lazy val demo = project
  .settings(
    name := "spark-experiments-demo",
    commonSettings,
    libraryDependencies ++= commonDependencies
  ).dependsOn(core)

lazy val ml = project
    .settings(
      name := "spark-experiments-ml",
      commonSettings,
      libraryDependencies ++= commonDependencies ++ Seq(
        "org.apache.spark" %% "spark-mllib" % sparkVersion
      )
    ).dependsOn(core)

lazy val streaming = project
    .settings(
      name := "spark-experiments-streaming",
      commonSettings,
      libraryDependencies ++= commonDependencies
    ).dependsOn(core)

// ========
// SETTINGS
// ========

resolvers += Resolver.sonatypeRepo("releases")
//addCompilerPlugin("org.typelevel" % "kind-projector" % "0.13.2" cross CrossVersion.full)

lazy val commonDependencies = Seq(
  "org.apache.spark" %% "spark-core" % sparkVersion % "provided",
  "org.apache.spark" %% "spark-sql" % sparkVersion % "provided"
)

val meta = """META.INF(.)*""".r
lazy val commonSettings = Seq(
  scalacOptions += "-Ywarn-unused:imports",
  assembly / assemblyMergeStrategy := {
    case PathList("javax", "servlet", xs @ _*) => MergeStrategy.first
    case PathList(ps @ _*) if ps.last endsWith ".html" => MergeStrategy.first
    case n if n.contains("services") => MergeStrategy.concat
    case n if n.startsWith("reference.conf") => MergeStrategy.concat
    case n if n.endsWith(".conf") => MergeStrategy.concat
    case meta(_) => MergeStrategy.discard
    case _ => MergeStrategy.first
  }
)
