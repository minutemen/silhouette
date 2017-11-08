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

import silhouette.exceptions.TransformException
import silhouette.http.transport.format.BearerAuthHeaderFormat._
import silhouette.http.{ Reads, Writes }

import scala.util.{ Failure, Success, Try }

/**
 * Handles the transformation of the "bearer" `Authorization` header.
 */
final case class BearerAuthHeaderFormat() extends Reads[String] with Writes[String] {

  /**
   * Transforms the "bearer" `Authorization` header value into some token.
   *
   * @param header The "bearer" `Authorization` header value.
   * @return Some token or a failure if the header could not be parsed.
   */
  override def read(header: String): Try[String] = {
    if (header.startsWith("Bearer ")) {
      Success(header.replace("Bearer ", ""))
    } else {
      Failure(new TransformException(MissingBearerAuthIdentifier))
    }
  }

  /**
   * Transforms a token into a "bearer" `Authorization` header value.
   *
   * @param token The token to encode.
   * @return The "bearer" `Authorization` header value.
   */
  override def write(token: String): String = s"Bearer $token"
}

/**
 * The companion object of the [[BearerAuthHeaderFormat]] class.
 */
object BearerAuthHeaderFormat {
  val MissingBearerAuthIdentifier = "Header doesn't start with 'Bearer '"
}
