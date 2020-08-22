import sbt.Keys.resolvers

ThisBuild / version := "0.1"

ThisBuild / scalaVersion := "2.12.8"

val sparkVersion = "3.0.0"

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
addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.9")

lazy val commonDependencies = Seq(
  "org.apache.spark" %% "spark-core" % sparkVersion,
  "org.apache.spark" %% "spark-sql" % sparkVersion
)

lazy val commonSettings = Seq(
  scalacOptions += "-Ypartial-unification",
  fullClasspath in Runtime := (fullClasspath in (Compile, run)).value,
  assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false),
  assemblyMergeStrategy in assembly := {
    case PathList("org","aopalliance", xs @ _*) => MergeStrategy.last
    case PathList("javax", "inject", xs @ _*) => MergeStrategy.last
    case PathList("javax", "servlet", xs @ _*) => MergeStrategy.last
    case PathList("javax", "activation", xs @ _*) => MergeStrategy.last
    case PathList("org", "apache", xs @ _*) => MergeStrategy.last
    case PathList("com", "google", xs @ _*) => MergeStrategy.last
    case PathList("com", "esotericsoftware", xs @ _*) => MergeStrategy.last
    case PathList("com", "codahale", xs @ _*) => MergeStrategy.last
    case PathList("com", "yammer", xs @ _*) => MergeStrategy.last
    case "about.html" => MergeStrategy.rename
    case "META-INF/ECLIPSEF.RSA" => MergeStrategy.last
    case "META-INF/mailcap" => MergeStrategy.last
    case "META-INF/mimetypes.default" => MergeStrategy.last
    case "plugin.properties" => MergeStrategy.last
    case "log4j.properties" => MergeStrategy.last
    case "overview.html" => MergeStrategy.last  // Added this for 2.1.0 I think
    case x =>
      val oldStrategy = (assemblyMergeStrategy in assembly).value
      oldStrategy(x)
  }
)
