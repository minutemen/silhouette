/**
 * Licensed to the Minutemen Group under one or more contributor license
 * agreements. See the COPYRIGHT file distributed with this work for
 * additional information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License. You may
 * obtain a copy of the License at
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

  object Version {
    val specs2 = "4.6.0"
    val circe = "0.13.0"
    val cats = "2.1.4"
    val sttp = "2.1.0-RC1"
    val silencer = "1.7.0"
  }

  val resolvers = Seq()

  object Library {

    object Specs2 {
      val core = "org.specs2" %% "specs2-core" % Version.specs2
      val matcherExtra = "org.specs2" %% "specs2-matcher-extra" % Version.specs2
      val mock = "org.specs2" %% "specs2-mock" % Version.specs2
    }

    object Circe {
      val core = "io.circe" %% "circe-core" % Version.circe
      val parser = "io.circe" %% "circe-parser" % Version.circe
    }

    object Cats {
      val core = "org.typelevel" %% "cats-core" % Version.cats
      val effect = "org.typelevel" %% "cats-effect" % Version.cats
    }

    object Sttp {
      val core = "com.softwaremill.sttp.client" %% "core" % Version.sttp
      val circe = "com.softwaremill.sttp.client" %% "circe" % Version.sttp
      val cats = "com.softwaremill.sttp.client" %% "async-http-client-backend-cats" % Version.sttp
    }

    object Silencer {
      val compiler = compilerPlugin(("com.github.ghik" % "silencer-plugin" % Version.silencer).cross(CrossVersion.full))
      val lib = ("com.github.ghik" % "silencer-lib" % Version.silencer % Provided).cross(CrossVersion.full)
    }

    val jbcrypt = "org.mindrot" % "jbcrypt" % "0.3m"
    val slf4jApi = "org.slf4j" % "slf4j-api" % "1.7.13"
    val inject = "javax.inject" % "javax.inject" % "1"
    val commonCodec = "commons-codec" % "commons-codec" % "1.10"
    val jose4j = "org.bitbucket.b_c" % "jose4j" % "0.7.2"
    val bouncyCastle = "org.bouncycastle" % "bcprov-jdk15on" % "1.66"
    val scalaXml = "org.scala-lang.modules" %% "scala-xml" % "1.2.0"
    val collectionCombat = "org.scala-lang.modules" %% "scala-collection-compat" % "2.1.6"
    val scalaLogging = "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2"
    val logbackClassic = "ch.qos.logback" % "logback-classic" % "1.2.3"
  }
}
