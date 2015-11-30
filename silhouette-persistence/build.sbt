import Dependencies._

libraryDependencies ++= Seq(
  Library.inject,
  Library.Specs2.core % Test,
  Library.Specs2.matcherExtra % Test,
  Library.Specs2.mock % Test,
  Library.scalaGuice % Test
)

enablePlugins(Doc)
