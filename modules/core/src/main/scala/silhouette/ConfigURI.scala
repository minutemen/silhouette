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
package silhouette

import java.net.URI

import scala.language.implicitConversions

/**
 * An URI that can have format parameters in the URI that will be replaced at runtime.
 *
 * If a [[java.net.URI]] contains format parameters like `%s` it will throw an exception on instantiation. This
 * class allows us to specify an URI with such parameters. We will provide implicit converters that help to
 * translate between this class and [[java.net.URI]].
 *
 * @param uri An URI.
 */
case class ConfigURI(uri: String) {

  /**
   * Uses the underlying string as a pattern (in a fashion similar to printf in C), and uses the supplied arguments
   * to fill in the holes.
   *
   * @param args The arguments used to instantiating the pattern.
   */
  def format(args: Any*): ConfigURI = ConfigURI(uri.format(args: _*))

  /**
   * Converts this instance to a [[java.net.URI]] instance.
   *
   * @return A [[java.net.URI]] instance.
   */
  def toURI: URI = new URI(uri)

  /**
   * Gets the string representation of the URI.
   *
   * @return The string representation of the URI.
   */
  override def toString: String = uri
}

/**
 * The companion object.
 */
object ConfigURI {

  /**
   * Converts a [[ConfigURI]] to a [[java.net.URI]].
   *
   * @param uri The URI to convert.
   * @return A [[java.net.URI]] instance.
   */
  implicit def toURI(uri: ConfigURI): URI = uri.toURI
}
