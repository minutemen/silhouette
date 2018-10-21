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

import silhouette.http.auth.BasicAuthSchemeFormat._
import silhouette.http.{ AuthScheme, BasicCredentials }
import silhouette.{ Reads, TransformException, Writes }

import scala.util.{ Failure, Success, Try }

/**
 * Handles the transformation of the "Basic" `Authorization` header to [[BasicCredentials]] and vice versa.
 */
final case class BasicAuthSchemeFormat()
  extends Reads[String, Try[BasicCredentials]]
  with Writes[BasicCredentials, String] {

  /**
   * Transforms the "Basic" `Authorization` header value into some [[BasicCredentials]].
   *
   * @param value The "basic" `Authorization` header value.
   * @return Some [[BasicCredentials]] on success or a failure if the value could not be parsed.
   */
  override def read(value: String): Try[BasicCredentials] = value match {
    case AuthScheme.Basic(payload) => payload match {
      case BasicCredentials(credentials) =>
        Success(credentials)
      case _ =>
        Failure(new TransformException(InvalidBasicAuthHeader))
    }
    case _ =>
      Failure(new TransformException(MissingBasicAuthIdentifier))
  }

  /**
   * Transforms [[BasicCredentials]] into a "Basic" `Authorization` header value.
   *
   * @param credentials The credentials to encode.
   * @return The "Basic" `Authorization` header value.
   */
  override def write(credentials: BasicCredentials): String = {
    AuthScheme.Basic(BasicCredentials(credentials))
  }
}

/**
 * The companion object of the [[BasicAuthSchemeFormat]] class.
 */
object BasicAuthSchemeFormat {
  val MissingBasicAuthIdentifier = "Header doesn't start with 'Basic '"
  val InvalidBasicAuthHeader = "A 'Basic' auth header must consists of two parts divided by a colon"
}
