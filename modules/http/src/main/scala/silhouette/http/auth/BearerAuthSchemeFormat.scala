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
package silhouette.http.auth

import silhouette.http.auth.BearerAuthSchemeFormat._
import silhouette.http.{ AuthScheme, BearerToken }
import silhouette.{ Reads, TransformException, Writes }

import scala.util.{ Failure, Success, Try }

/**
 * Handles the transformation of the "Bearer" `Authorization` header to a [[BearerToken]] and vice versa.
 */
final case class BearerAuthSchemeFormat() extends Reads[String, Try[BearerToken]] with Writes[BearerToken, String] {

  /**
   * Transforms the "Bearer" `Authorization` header value into some token.
   *
   * @param value The "Bearer" `Authorization` header value.
   * @return Some token or a failure if the value could not be parsed.
   */
  override def read(value: String): Try[BearerToken] = value match {
    case AuthScheme.Bearer(token) =>
      Success(BearerToken(token))
    case _ =>
      Failure(new TransformException(MissingBearerAuthIdentifier))
  }

  /**
   * Transforms a token into a "Bearer" `Authorization` header value.
   *
   * @param token The token to encode.
   * @return The "Bearer" `Authorization` header value.
   */
  override def write(token: BearerToken): String = AuthScheme.Bearer(token.value)
}

/**
 * The companion object of the [[BearerAuthSchemeFormat]] class.
 */
object BearerAuthSchemeFormat {
  val MissingBearerAuthIdentifier = "Header doesn't start with 'Bearer '"
}
