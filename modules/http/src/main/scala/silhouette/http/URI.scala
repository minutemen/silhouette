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
package silhouette.http

import java.net.{ URI => JavaURI }

import scala.language.implicitConversions

/**
 * An URI implementation that can have format parameters in the URI that will be replaced at runtime.
 *
 * If a [[java.net.URI]] contains format parameters like `%s` it will throw an exception on instantiation. This
 * class allows us to specify an URI with such parameters. We will provide implicit converters that help to
 * translate between this class and [[java.net.URI]].
 *
 * @param uri An URI.
 */
case class URI(uri: String) {

  /**
   * Uses the underlying string as a pattern (in a fashion similar to printf in C), and uses the supplied arguments
   * to fill in the holes.
   *
   * @see [[scala.collection.immutable.StringLike.format]]
   * @param args The arguments used to instantiating the pattern.
   */
  def format(args: Any*): URI = URI(uri.format(args: _*))

  /**
   * Converts this instance to a [[java.net.URI]] instance.
   *
   * @return A [[java.net.URI]] instance.
   */
  def toJava: JavaURI = new JavaURI(uri)

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
object URI {

  /**
   * Converts a [[silhouette.http.URI]] to a [[java.net.URI]].
   *
   * @param uri The URI to convert.
   * @return A [[java.net.URI]] instance.
   */
  implicit def toJavaURI(uri: URI): JavaURI = uri.toJava
}
