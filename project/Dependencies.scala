/**
 * Copyright 2015 Mohiva Organisation (license at mohiva dot com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import sbt._

object Dependencies {

  object Versions {
    val crossScala = Seq("2.11.8")
    val scalaVersion = crossScala.head
  }

  val resolvers = Seq()

  object Library {

    object Specs2 {
      private val version = "3.6.5"
      val core = "org.specs2" %% "specs2-core" % version
      val matcherExtra = "org.specs2" %% "specs2-matcher-extra" % version
      val mock = "org.specs2" %% "specs2-mock" % version
    }

    val mockito = "org.mockito" % "mockito-core" % "1.10.19"
    val jbcrypt = "org.mindrot" % "jbcrypt" % "0.3m"
    val scalaGuice = "net.codingwell" %% "scala-guice" % "4.0.0"
    val playJson = "com.typesafe.play" % "play-json_2.11" % "2.4.4"
    val slf4jApi = "org.slf4j" % "slf4j-api" % "1.7.13"
    val inject = "javax.inject" % "javax.inject" % "1"
    val commonCodec = "commons-codec" % "commons-codec" % "1.10"
  }
}
