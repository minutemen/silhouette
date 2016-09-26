import Dependencies._

libraryDependencies ++= Seq(
  Library.inject,
  Library.scalaGuice % Test
)

enablePlugins(Doc)
