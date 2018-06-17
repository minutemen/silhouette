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

lazy val `silhouette-specs2` = project in file("modules/specs2")

lazy val `silhouette-core` = (project in file("modules/core"))
  .dependsOn(
    `silhouette-specs2` % Test
  )

lazy val `silhouette-crypto` = (project in file("modules/crypto"))
  .dependsOn(
    `silhouette-core`,
    `silhouette-specs2` % Test
  )

lazy val `silhouette-crypto-jca` = (project in file("modules/crypto-jca"))
  .dependsOn(
    `silhouette-core`,
    `silhouette-crypto`,
    `silhouette-specs2` % Test
  )

lazy val `silhouette-http` = (project in file("modules/http"))
  .dependsOn(
    `silhouette-core`,
    `silhouette-crypto`,
    `silhouette-specs2` % Test
  )

lazy val `silhouette-jwt` = (project in file("modules/jwt"))
  .dependsOn(
    `silhouette-core`,
    `silhouette-specs2` % Test
  )

lazy val `silhouette-jwt-jose4j` = (project in file("modules/jwt-jose4j"))
  .dependsOn(
    `silhouette-core`,
    `silhouette-crypto`,
    `silhouette-jwt`,
    `silhouette-specs2` % Test
  )

lazy val `silhouette-authenticator` = (project in file("modules/authenticator"))
  .dependsOn(
    `silhouette-core`,
    `silhouette-crypto`,
    `silhouette-http`,
    `silhouette-jwt`,
    `silhouette-specs2` % Test
  )

lazy val `silhouette-password` = (project in file("modules/password"))
  .dependsOn(
    `silhouette-core`,
    `silhouette-crypto`,
    `silhouette-specs2` % Test
  )

lazy val `silhouette-password-bcrypt` = (project in file("modules/password-bcrypt"))
  .dependsOn(
    `silhouette-core`,
    `silhouette-password`,
    `silhouette-specs2` % Test
  )

lazy val `silhouette-provider` = (project in file("modules/provider"))
  .dependsOn(
    `silhouette-core`,
    `silhouette-http`,
    `silhouette-specs2` % Test
  )

lazy val `silhouette-provider-social` = (project in file("modules/provider-social"))
  .dependsOn(
    `silhouette-core`,
    `silhouette-provider`,
    `silhouette-crypto`,
    `silhouette-specs2` % Test
  )

lazy val `silhouette-provider-oauth2` = (project in file("modules/provider-oauth2"))
  .dependsOn(
    `silhouette-core`,
    `silhouette-provider`,
    `silhouette-provider-social` % "compile->compile;test->test",
    `silhouette-crypto`,
    `silhouette-specs2` % Test
  )

lazy val `silhouette-persistence` = (project in file("modules/persistence"))
  .dependsOn(
    `silhouette-core`,
    `silhouette-password`,
    `silhouette-specs2` % Test
  )

lazy val `silhouette-util` = (project in file("modules/util"))
  .dependsOn(
    `silhouette-core`,
    `silhouette-specs2` % Test
  )

lazy val silhouette = (project in file("."))
  .aggregate(
    `silhouette-specs2`,
    `silhouette-core`,
    `silhouette-crypto`,
    `silhouette-crypto-jca`,
    `silhouette-http`,
    `silhouette-jwt`,
    `silhouette-jwt-jose4j`,
    `silhouette-authenticator`,
    `silhouette-password`,
    `silhouette-password-bcrypt`,
    `silhouette-provider`,
    `silhouette-provider-social`,
    `silhouette-provider-oauth2`,
    `silhouette-persistence`,
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
