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

lazy val buildVersions = taskKey[Unit]("Show some build versions")

lazy val `silhouette-authenticator` = (project in file("silhouette-authenticator"))
  .dependsOn(`silhouette-core`, `silhouette-crypto`, `silhouette-http`, `silhouette-jwt`, `silhouette-specs2` % Test)

lazy val `silhouette-core` = (project in file("silhouette-core"))
  .dependsOn(`silhouette-specs2` % Test)

lazy val `silhouette-crypto` = (project in file("silhouette-crypto"))
  .dependsOn(`silhouette-core`, `silhouette-specs2` % Test)

lazy val `silhouette-crypto-jca` = (project in file("silhouette-crypto-jca"))
  .dependsOn(`silhouette-core`, `silhouette-crypto`, `silhouette-specs2` % Test)

lazy val `silhouette-http` = (project in file("silhouette-http"))
  .dependsOn(`silhouette-core`, `silhouette-crypto`, `silhouette-specs2` % Test)

lazy val `silhouette-jwt` = (project in file("silhouette-jwt"))
  .dependsOn(`silhouette-core`, `silhouette-specs2` % Test)

lazy val `silhouette-jwt-jose4j` = (project in file("silhouette-jwt-jose4j"))
  .dependsOn(`silhouette-core`, `silhouette-crypto`, `silhouette-jwt`, `silhouette-specs2` % Test)

lazy val `silhouette-password` = (project in file("silhouette-password"))
  .dependsOn(`silhouette-core`, `silhouette-crypto`, `silhouette-specs2` % Test)

lazy val `silhouette-password-bcrypt` = (project in file("silhouette-password-bcrypt"))
  .dependsOn(`silhouette-core`, `silhouette-password`, `silhouette-specs2` % Test)

lazy val `silhouette-provider` = (project in file("silhouette-provider"))
  .dependsOn(`silhouette-core`, `silhouette-http`, `silhouette-specs2` % Test)

lazy val `silhouette-persistence` = (project in file("silhouette-persistence"))
  .dependsOn(`silhouette-core`, `silhouette-password`, `silhouette-specs2` % Test)

lazy val `silhouette-specs2` = project in file("silhouette-specs2")

lazy val `silhouette-util` = (project in file("silhouette-util"))
  .dependsOn(`silhouette-core`, `silhouette-specs2` % Test)

lazy val silhouette = (project in file("."))
  .aggregate(
    `silhouette-authenticator`,
    `silhouette-core`,
    `silhouette-crypto`,
    `silhouette-crypto-jca`,
    `silhouette-http`,
    `silhouette-jwt`,
    `silhouette-jwt-jose4j`,
    `silhouette-password`,
    `silhouette-password-bcrypt`,
    `silhouette-provider`,
    `silhouette-persistence`,
    `silhouette-specs2`,
    `silhouette-util`
  ).settings(
    publish := {},
    buildVersions := {
      // scalastyle:off println
      println(s"PROJECT_VERSION ${version.value}")
      println(s"SCALA_VERSION ${scalaVersion.value}")
      // scalastyle:on println
    }
  )
