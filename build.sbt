name := "Etri Project"
version := "1.0"
scalaVersion := "2.12.10"
val sparkVersion = "3.1.2"
libraryDependencies ++= Seq(
        "org.apache.spark" %% "spark-core" % "3.1.2",
	"org.scala-lang" % "scala-compiler" % "2.12.10",
	"com.google.guava" % "guava" % "11.0.2",
	"commons-net" % "commons-net" % "2.2",
	"com.google.code.findbugs" % "jsr305" % "1.3.9"	,
	"org.rogach" %% "scallop" % "3.1.3"
)

resolvers += "MavenRepository" at "https://mvnrepository.com/"
autoScalaLibrary := false

