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

////*******************************
//// Basic settings
////*******************************
object BasicSettings extends AutoPlugin {
  override def trigger: PluginTrigger = allRequirements

  val `scalacOptions2.12.x` = Seq(
    "-Xlint:adapted-args",
    "-Ywarn-inaccessible",
    "-Ywarn-infer-any",
    "-Xlint:nullary-override",
    "-Xlint:nullary-unit",
    "-Xmax-classfile-name", "254",
    "-language:higherKinds",
    "-Ypartial-unification"
  )

  val `scalacOptions2.13.x` = Seq(
    "-Xlint:adapted-args",
    "-Xlint:inaccessible",
    "-Xlint:infer-any",
    "-Xlint:nullary-override",
    "-Xlint:nullary-unit"
  )

  val scalacOptionsCommon = Seq(
    "-unchecked",
    "-deprecation",
    "-feature",
    "-encoding", "utf8",
    "-Xfatal-warnings",
    "-Xlint"
  )

  override def projectSettings: Seq[Setting[_]] = Seq(
    organization := "group.minutemen",
    resolvers ++= Dependencies.resolvers,
    scalaVersion := crossScalaVersions.value.head,
    crossScalaVersions := Seq("2.13.2", "2.12.11"),
    scalacOptions ++= {
      scalacOptionsCommon ++ (scalaBinaryVersion.value match {
        case "2.12" => `scalacOptions2.12.x`
        case "2.13" => `scalacOptions2.13.x`
      })
    },
    scalacOptions in Test ~= { options: Seq[String] =>
      options filterNot (_ == "-Ywarn-dead-code") // Allow dead code in tests (to support using mockito).
    },
    parallelExecution in Test := false,
    fork in Test := true,
    // Needed to avoid https://github.com/travis-ci/travis-ci/issues/3775 in forked tests
    // in Travis with `sudo: false`.
    // See https://github.com/sbt/sbt/issues/653
    // and https://github.com/travis-ci/travis-ci/issues/3775
    javaOptions += "-Xmx1G"
  )
}

////*******************************
//// Doc settings
////*******************************
/*object Doc extends AutoPlugin {

  import com.typesafe.sbt.sbtghpages.GhpagesPlugin._
  import com.typesafe.sbt.SbtGit.GitKeys._
  import com.typesafe.sbt.SbtGit.git
  import com.typesafe.sbt.SbtSite.SiteKeys._
  import com.typesafe.sbt.SbtSite.site
  import sbtunidoc.Plugin._

  lazy val files = Seq(file("CNAME"))

  lazy val scalaDocSettings = Seq(
    autoAPIMappings := true,
    apiURL := Some(url(s"http://api.silhouette.rocks/${version.value}/"))
  )

  lazy val apiDocPublishSettings = Seq(
    // https://github.com/paypal/horizon/blob/develop/src/main/scala/com/paypal/horizon/BuildUtilities.scala
    // Create version
    siteMappings <++= (mappings in (ScalaUnidoc, packageDoc), version).map { (mapping, ver) =>
      for ((file, path) <- mapping) yield (file, s"$ver/$path")
    },
    // Add custom files from site directory
    siteMappings <++= baseDirectory.map { dir =>
      for (file <- files) yield (new File(dir.getAbsolutePath + "/site/" + file), file.name)
    },
    // Do not delete old versions
    synchLocal <<= (privateMappings, updatedRepository, gitRunner, streams).map { (mappings, repo, git, s) =>
      val betterMappings = mappings.map { case (file, tgt) => (file, repo / tgt) }
      IO.copy(betterMappings)
      repo
    },
    git.remoteRepo := "git@github.com:minutemen/silhouette.git"
  )

  override def projectSettings: Seq[Setting[_]] =
    unidocSettings ++
    site.settings ++
    ghpagesProjectSettings ++
    scalaDocSettings ++
    apiDocPublishSettings
}*/

////*******************************
//// Publish settings
////*******************************
object Publish extends AutoPlugin {

  import xerial.sbt.Sonatype._
  import SonatypeKeys._

  override def trigger: PluginTrigger = allRequirements

  private val pom = {
    <scm>
      <url>git@github.com:minutemen/silhouette.git</url>
      <connection>scm:git:git@github.com:minutemen/silhouette.git</connection>
    </scm>
      <developers>
        <developer>
          <id>akkie</id>
          <name>Christian Kaps</name>
          <url>http://mohiva.com</url>
        </developer>
      </developers>
  }


  override def projectSettings: Seq[Setting[_]] = sonatypeSettings ++ Seq(
    description := "Framework agnostic authentication library for Scala that supports several authentication " +
      "methods, including OAuth2, OpenID Connect, CAS, Credentials, Basic Authentication " +
      "or custom authentication schemes",
    homepage := Some(url("http://www.silhouette.rocks/")),
    licenses := Seq("Apache-2.0" -> url("https://github.com/minutemen/silhouette/blob/master/LICENSE")),
    publishMavenStyle := true,
    publishArtifact in Test := false,
    pomIncludeRepository := { _ => false },
    pomExtra := pom,
    publishTo := sonatypePublishTo.value,
    credentials ++= (for {
      username <- Option(System.getenv().get("SONATYPE_USERNAME"))
      password <- Option(System.getenv().get("SONATYPE_PASSWORD"))
    } yield Credentials("Sonatype Nexus Repository Manager", "oss.sonatype.org", username, password)).toSeq
  )
}

////*******************************
//// Release settings
////*******************************
/*object Release extends AutoPlugin {

  import sbtrelease.ReleasePlugin.autoImport._
  import ReleaseTransformations._

  override def trigger: PluginTrigger = allRequirements

  override def projectSettings: Seq[Setting[_]] = Seq(
    releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      runClean,
      runTest,
      setReleaseVersion,
      commitReleaseVersion,
      tagRelease,
      ReleaseStep(
        action = Command.process("publishSigned", _),
        enableCrossBuild = crossScalaVersions.value.length > 1
      ),
      setNextVersion,
      commitNextVersion,
      ReleaseStep(
        action = Command.process("sonatypeReleaseAll", _),
        enableCrossBuild = crossScalaVersions.value.length > 1
      ),
      pushChanges
    )
  )
}
*/
