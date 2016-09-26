import Dependencies._

libraryDependencies ++= Seq(
  Library.Specs2.core,
  Library.Specs2.matcherExtra,
  Library.Specs2.mock,
  Library.mockito
)
enablePlugins(Doc)
