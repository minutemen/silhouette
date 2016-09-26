import Dependencies._

libraryDependencies ++= Seq(
  Library.playJson,
  Library.slf4jApi,
  Library.inject,
  Library.commonCodec,
  Library.scalaGuice % Test
)
enablePlugins(Doc)
