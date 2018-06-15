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
package silhouette.http.transport.format

import silhouette.crypto.Base64
import silhouette.http.transport.format.BasicAuthHeaderFormat._
import silhouette.{ Credentials, Reads, TransformException, Writes }

import scala.util.{ Failure, Success, Try }

/**
 * Handles the transformation of the "basic" `Authorization` header.
 */
final case class BasicAuthHeaderFormat() extends Reads[String, Try[Credentials]] with Writes[Credentials, String] {

  /**
   * Transforms the "basic" `Authorization` header value into some [[Credentials]].
   *
   * @param header The "basic" `Authorization` header value.
   * @return Some [[Credentials]] on success or a failure if the header could not be parsed.
   */
  override def read(header: String): Try[Credentials] = {
    if (header.startsWith("Basic ")) {
      Base64.decode(header.replace("Basic ", "")).split(":", 2) match {
        case Array(identifier, password) => Success(Credentials(identifier, password))
        case _                           => Failure(new TransformException(InvalidBasicAuthHeader))
      }
    } else {
      Failure(new TransformException(MissingBasicAuthIdentifier))
    }
  }

  /**
   * Transforms [[Credentials]] into a "basic" `Authorization` header value.
   *
   * @param credentials The credentials to encode.
   * @return The "basic" `Authorization` header value.
   */
  override def write(credentials: Credentials): String = {
    s"Basic ${Base64.encode(s"${credentials.identifier}:${credentials.password}")}"
  }
}

/**
 * The companion object of the [[BasicAuthHeaderFormat]] class.
 */
object BasicAuthHeaderFormat {
  val MissingBasicAuthIdentifier = "Header doesn't start with 'Basic '"
  val InvalidBasicAuthHeader = "A basic auth header must consists of two parts divided by a colon"
}
