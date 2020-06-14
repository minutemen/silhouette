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

import silhouette.Credentials
import silhouette.crypto.Base64

import scala.language.implicitConversions
import scala.util.{ Success, Try }

/**
 * Credentials for authenticating with the HTTP protocol.
 *
 * @param authScheme The auth scheme.
 */
sealed abstract class HttpCredentials(val authScheme: AuthScheme) extends Credentials

/**
 * Credentials for the "Basic" authentication scheme.
 *
 * @see https://tools.ietf.org/html/rfc7617
 *
 * @param username The username.
 * @param password The password.
 */
case class BasicCredentials(username: String, password: String) extends HttpCredentials(AuthScheme.Basic)

/**
 * The companion object of the [[BasicCredentials]].
 */
object BasicCredentials {

  /**
   * Encodes the value of "Basic" authentication scheme and transforms it to [[BasicCredentials]].
   *
   * The value of the "Basic" authentication scheme is in the form "base64(username:password)". This method
   * extracts the username and the password and applies it as parameters to a [[BasicCredentials]] instance.
   *
   * @param value The decoded value of a "Basic" authentication scheme.
   * @return Maybe some [[BasicCredentials]] if the values could be extracted, None otherwise.
   */
  def unapply(value: String): Option[BasicCredentials] = Try(Base64.decode(value).split(":", 2)) match {
    case Success(Array(identifier, password)) =>
      Some(BasicCredentials(identifier, password))
    case _ =>
      None
  }

  /**
   * Transforms [[BasicCredentials]] into the value of a "Basic" authentication scheme.
   *
   * The value of the "Basic" authentication scheme is in the form "base64(username:password)". This method
   * creates the string representation of the [[BasicCredentials]] exactly in the this form.
   *
   * @param credentials The the basic credentials instance to transform.
   * @return The credentials in the form "base64(username:password)".
   */
  def apply(credentials: BasicCredentials): String = Base64.encode(s"${credentials.username}:${credentials.password}")
}

/**
 * Credentials for the "Bearer" token authentication scheme.
 *
 * @param value The token value.
 */
case class BearerToken(value: String) extends HttpCredentials(AuthScheme.Bearer)

/**
 * The companion object of the [[BearerToken]].
 */
object BearerToken {

  /**
   * Instantiates a [[BearerToken]] from a string.
   *
   * @param token The bearer token as string.
   * @return A [[BearerToken]] instance.
   */
  implicit def strToBearerToken(token: String): BearerToken = BearerToken(token)
}
