import Dependencies._

libraryDependencies ++= Seq(
  Library.jwtCore,
  Library.jwtApi,
  Library.jodaTime,
  Library.playJson,
  Library.slf4jApi,
  Library.Specs2.core % Test,
  Library.Specs2.matcherExtra % Test,
  Library.Specs2.mock % Test,
  Library.scalaGuice % Test
)
enablePlugins(Doc)
