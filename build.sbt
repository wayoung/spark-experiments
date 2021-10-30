import sbt.Keys.resolvers

ThisBuild / version := "0.1"

ThisBuild / scalaVersion := "2.13.6"

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

lazy val commonSettings = Seq(
  scalacOptions += "-Ywarn-unused:imports",
  fullClasspath in Runtime := (fullClasspath in (Compile, run)).value,
  assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false),
  assemblyMergeStrategy in assembly := {
    case PathList("META-INF", xs @ _*) => MergeStrategy.discard
    case PathList("javax", xs @ _*) => MergeStrategy.last
    case PathList("com", "google", xs @ _*) => MergeStrategy.last
    case PathList("com", "esotericsoftware", xs @ _*) => MergeStrategy.last
    case PathList("com", "codahale", xs @ _*) => MergeStrategy.last
    case PathList("com", "yammer", xs @ _*) => MergeStrategy.last
    case PathList("com", "fasterxml", xs @ _*) => MergeStrategy.last
    case PathList("dev", "ludovic", xs @ _*) => MergeStrategy.last
    case PathList("io", "netty", xs @ _*) => MergeStrategy.last
    case PathList("org", "apache", xs @ _*) => MergeStrategy.last
    case PathList("org","aopalliance", xs @ _*) => MergeStrategy.last
    case "module-info.class" => MergeStrategy.last
    case "plugin.properties" => MergeStrategy.last
    case "log4j.properties" => MergeStrategy.last
    case "git.properties" => MergeStrategy.last
    case x =>
      val oldStrategy = (assemblyMergeStrategy in assembly).value
      oldStrategy(x)
  }
)
