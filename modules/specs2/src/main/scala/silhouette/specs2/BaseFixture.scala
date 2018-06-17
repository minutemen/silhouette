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
package silhouette.specs2

import java.io.{ FileNotFoundException, InputStream }
import java.net.URL
import java.nio.file.{ FileSystems, Path, Paths }

import io.circe.parser.parse
import io.circe.{ Decoder, Json }

import scala.xml.{ Elem, XML }

/**
 * Fixture helpers.
 */
trait BaseFixture {
  type F <: BaseFixtureConverter

  /**
   * The path separator.
   */
  val Separator: String = FileSystems.getDefault.getSeparator

  /**
   * Converts the fixture in different types.
   *
   * Use lazy initialization for the methods that read from input stream, so the input stream needn't be reset.
   * Moreover, the operation is deterministic for the same input stream, so we can reuse the result, instead of
   * parse it again.
   */
  trait BaseFixtureConverter {
    val inputStream: InputStream
    val path: Path

    lazy val asString: String = scala.io.Source.fromInputStream(inputStream)(scala.io.Codec("UTF-8")).mkString
    lazy val asJson: Json = parse(asString).getOrElse(throw new RuntimeException(s"Cannot parse Json from: $asString"))
    lazy val asXml: Elem = XML.load(inputStream)
    def as[T](implicit d: Decoder[T]): T = asJson.as[T].getOrElse(throw new RuntimeException("Cannot decode JSON"))
  }

  /**
   * Wraps the stream with the fixture converter.
   *
   * @param inputStream The input stream of the fixture.
   * @param path        The fixture path.
   * @return The test fixture.
   */
  def wrap(inputStream: InputStream, path: Path): F

  /**
   * Loads a test fixture.
   *
   * @param path The fixture path.
   * @return The test fixture.
   */
  def load(path: Path): F = load(this.getClass.getClassLoader, path)

  /**
   * Loads a test fixture.
   *
   * @param classLoader The class loader to use to load the fixture.
   * @param path        The fixture path.
   * @return The test fixture.
   */
  def load(classLoader: ClassLoader, path: Path): F = {
    wrap(Option(classLoader.getResourceAsStream(path.toString)) match {
      case Some(is) => is
      case None     => throw new FileNotFoundException("Cannot load test fixture: " + path)
    }, path)
  }

  /**
   * Gets a path for a fixture from class path.
   *
   * @param path The fixture path.
   * @return The path to the fixture.
   */
  def path(path: Path): Path = this.path(this.getClass.getClassLoader, path)

  /**
   * Gets a path for a fixture from class path.
   *
   * @param classLoader The class loader to use to load the fixture.
   * @param path        The fixture path.
   * @return The path to the fixture.
   */
  def path(classLoader: ClassLoader, path: Path): Path = {
    val crossPlatformPath = path.toString.stripPrefix(Separator).replace(Separator, "/")
    val url: URL = Option(classLoader.getResource(crossPlatformPath)).getOrElse {
      throw new FileNotFoundException("Cannot find test file: " + path)
    }

    // On Windows the URL will be prepended with a "/", so it will return /C:/ as example. We must remove the
    // prefixed slash, otherwise it's not a valid path and the Paths.get method throws an exception
    val windowsDirPattern = "/([A-Z]{1}:.*)".r
    url.getFile match {
      case windowsDirPattern(p) => Paths.get(p)
      case p                    => Paths.get(p)
    }
  }
}

/**
 * Base fixture helpers.
 */
object BaseFixture extends BaseFixture {
  override type F = FixtureConverter

  /**
   * Converts the fixture in different types.
   *
   * @param inputStream The input stream of the fixture.
   * @param path        The fixture path.
   */
  case class FixtureConverter(inputStream: InputStream, path: Path) extends BaseFixtureConverter

  /**
   * Wraps the stream with the fixture converter.
   *
   * @param inputStream The input stream of the fixture.
   * @param path        The fixture path.
   * @return The test fixture.
   */
  override def wrap(inputStream: InputStream, path: Path): F = FixtureConverter(inputStream, path)
}
