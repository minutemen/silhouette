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

import scala.util.Try

/**
 * Provides the HTTP client implementation.
 */
package object client {

  /**
   * Provides syntax via enrichment classes.
   *
   * @param body The body instance on which the additional methods should be defined.
   */
  implicit final class RichBody[A <: Body](body: A) {

    /**
     * Provides a method that transforms from `Body` to `T`.
     *
     * @param format An implicit `BodyFormat` instance that is used to transform the body.
     * @tparam T The type of the result.
     * @return An instance of T or an error if the body couldn't be transformed.
     */
    def as[T](implicit format: BodyFormat[T]): Try[T] = format.read(body)
  }
}
