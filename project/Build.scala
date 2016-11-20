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
import sbt.Keys._
import sbt._

object Build extends Build {
  lazy val buildVersions = taskKey[Unit]("Show some build versions")

  val silhouetteSpecs2 = Project(
    id = "silhouette-specs2",
    base = file("silhouette-specs2")
  )

  val silhouette = Project(
    id = "silhouette",
    base = file("silhouette"),
    dependencies = Seq(silhouetteSpecs2 % Test)
  )

  lazy val silhouetteCryptoJca = Project(
    id = "silhouette-crypto-jca",
    base = file("silhouette-crypto-jca"),
    dependencies = Seq(silhouette, silhouetteSpecs2 % Test)
  )

  val silhouettePasswordBcrypt = Project(
    id = "silhouette-password-bcrypt",
    base = file("silhouette-password-bcrypt"),
    dependencies = Seq(silhouette, silhouetteSpecs2 % Test)
  )

  val silhouettePersistence = Project(
    id = "silhouette-persistence",
    base = file("silhouette-persistence"),
    dependencies = Seq(silhouette, silhouetteSpecs2 % Test)
  )

  val root = Project(
    id = "root",
    base = file("."),
    aggregate = Seq(
      silhouette,
      silhouetteCryptoJca,
      silhouetteSpecs2,
      silhouettePasswordBcrypt,
      silhouettePersistence
    ),
    settings = Defaults.coreDefaultSettings ++
      APIDoc.settings ++
      Seq(
        publish := {},
        buildVersions := {
          // scalastyle:off println
          println(s"PROJECT_VERSION ${version.value}")
          println(s"SCALA_VERSION ${scalaVersion.value}")
          // scalastyle:on println
        }
      )
  )
}
