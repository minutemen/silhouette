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

import sttp.model.{ Header, HeaderNames }

/**
 * Represents an authorization header that is based on an [[AuthScheme]].
 *
 * @param scheme The auth scheme.
 */
sealed abstract class AuthorizationHeader(protected val scheme: AuthScheme) {

  /**
   * Creates an `Authorization` header with the value suffixed to the auth scheme.
   *
   * @param value The value to add to the auth scheme.
   * @return An `Authorization` header with the value suffixed to the auth scheme.
   */
  def apply(value: String): Header = Header.authorization(scheme.toString, value)
}

/**
 * An extractor object that handles the "Basic" `Authorization` header.
 */
object BasicAuthorizationHeader extends AuthorizationHeader(AuthScheme.Basic) {

  /**
   * Creates an "Basic" `Authorization` header from the given [[BasicCredentials]].
   *
   * @param credentials The credentials from which the header should be created.
   * @return A "Basic" `Authorization` header.
   */
  def apply(credentials: BasicCredentials): Header = apply(BasicCredentials(credentials))

  /**
   * Extracts [[BasicCredentials]] from an "Basic" `Authorization` header.
   *
   * The value of the "Basic" `Authorization` header is in the form "base64(username:password)". This method
   * extracts the username and the password and applies it as parameters to a [[BasicCredentials]] instance.
   *
   * @param header The header from which the [[BasicCredentials]] should be extracted.
   * @return Maybe some [[BasicCredentials]] if the values could be extracted, None otherwise.
   */
  def unapply(header: Header): Option[BasicCredentials] = header match {
    case Header(HeaderNames.Authorization, value) =>
      scheme.unapply(value).flatMap(BasicCredentials.unapply)
    case _ =>
      None
  }
}

/**
 * An extractor object that handles the "Bearer" `Authorization` header.
 */
object BearerAuthorizationHeader extends AuthorizationHeader(AuthScheme.Bearer) {

  /**
   * Creates a "Bearer" `Authorization` header from the given [[BearerToken]].
   *
   * @param token The token from which the header should be created.
   * @return A "Bearer" `Authorization` header.
   */
  def apply(token: BearerToken): Header = apply(token.value)

  /**
   * Extracts a [[BearerToken]] from a "Bearer" `Authorization` header.
   *
   * @param header The header from which the [[BearerToken]] should be extracted.
   * @return The extracted [[BearerToken]].
   */
  def unapply(header: Header): Option[BearerToken] = header match {
    case Header(HeaderNames.Authorization, value) =>
      scheme.unapply(value).map(BearerToken.apply)
    case _ =>
      None
  }
}
