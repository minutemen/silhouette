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

/**
 * Defines an HTTP authentication scheme.
 *
 * @param name The name of the scheme.
 */
sealed abstract class AuthScheme(name: String) {

  /**
   * Returns the string representation of the scheme.
   *
   * @return The string representation of the scheme.
   */
  override def toString: String = name

  /**
   * Creates a string representation of the scheme with the given value.
   *
   * @param value The value to append to the scheme.
   * @return The auth scheme in the form "[NAME] [VALUE]".
   */
  def apply(value: String): String = s"$name $value"

  /**
   * An extractor that extracts the value of the scheme.
   *
   * @param value The complete authentication scheme.
   * @return The value of the auth scheme.
   */
  def unapply(value: String): Option[String] =
    if (value.startsWith(s"$name "))
      Some(value.replace(s"$name ", ""))
    else
      None
}

/**
 * The companion object.
 */
object AuthScheme {

  /**
   * The 'Basic' authentication scheme.
   *
   * @see https://tools.ietf.org/html/rfc7617
   */
  case object Basic extends AuthScheme("Basic")

  /**
   * The 'Digest' authentication scheme.
   *
   * @see https://tools.ietf.org/html/rfc7616
   */
  case object Digest extends AuthScheme("Digest")

  /**
   * The "Bearer" token authentication scheme.
   */
  case object Bearer extends AuthScheme("Bearer")
}
