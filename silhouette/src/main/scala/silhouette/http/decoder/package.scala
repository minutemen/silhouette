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

import silhouette.http.client.Body
import scala.util.Try

/**
 * This package contains decode feature.
 */
package object decoder {

  /**
   * Provides syntax via enrichment classes.
   */
  implicit final class DecoderOps[A <: Body](val wrapped: A) {

    /**
     * Provide method that decode from Body to a T.
     *
     * @param decoder Decoder instance able to decode.
     * @tparam T The result type to decode.
     * @return an instance of T or an error if the body couldn't be decoded.
     */
    def as[T](implicit decoder: BodyDecoder[T]): Try[T] = decoder.decode(wrapped)
  }
}
