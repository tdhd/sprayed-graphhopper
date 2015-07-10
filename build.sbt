val project = Project(
  id = "sprayedgraphhopper-id",
  base = file("."),
  settings = Project.defaultSettings ++
             Seq(
               name := """sprayedgraphhopper""",
               scalaVersion := "2.11.6",
               scalacOptions in Compile ++= Seq("-encoding", "UTF-8", "-target:jvm-1.8", "-deprecation", "-feature", "-unchecked", "-Xlog-reflective-calls", "-Xlint"),
               javaOptions in run ++= Seq("-Xms128m", "-Xmx1024m"),
               libraryDependencies ++= Seq(
                 "com.graphhopper" % "graphhopper" % "0.4.1",
                 "com.typesafe.akka" %% "akka-actor" % "2.3.11",
                 "io.spray" %% "spray-can" % "1.3.3",
                 "io.spray" %% "spray-routing" % "1.3.3",
                 "io.spray" %% "spray-httpx" % "1.3.3",
                 "io.spray" %%  "spray-json" % "1.3.2",
                 "org.scala-lang.modules" %% "scala-xml" % "1.0.3",
                 "com.github.scopt" %% "scopt" % "3.3.0")
             )
)
