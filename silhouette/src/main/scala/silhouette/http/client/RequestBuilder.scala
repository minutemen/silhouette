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
package silhouette.http.client

import scala.concurrent.Future

/**
 * Builds a request by providing a fluent interface.
 *
 * An instance of this class should not be instantiated directly. Instead the [[HttpClient]] should be
 * used to instantiate the [[RequestBuilder]].
 */
private[silhouette] trait RequestBuilder {

  /**
   * The concrete implementation of this trait.
   */
  type Self <: RequestBuilder

  /**
   * Returns a copy of this instance with a new URL.
   *
   * @param url The URL to set.
   * @return A request builder to provide a fluent interface.
   */
  def withUrl(url: String): Self

  /**
   * Returns a copy of this instance with a new HTTP method.
   *
   * @param method The HTTP method to set.
   * @return A request builder to provide a fluent interface.
   */
  def withMethod(method: String): Self

  /**
   * Returns a copy of this instance with the list of headers set.
   *
   * @param headers The headers to set.
   * @return A request builder to provide a fluent interface.
   */
  def withHeaders(headers: (String, String)*): Self

  /**
   * Returns a copy of this instance with the list of query params set.
   *
   * @param params The query params to set.
   * @return A request builder to provide a fluent interface.
   */
  def withQueryParams(params: (String, String)*): Self

  /**
   * Returns a copy of this instance with a new body.
   *
   * @param body The body to set.
   * @return A request builder to provide a fluent interface.
   */
  def withBody(body: Body): Self

  /**
   * Returns a copy of this instance with a new body.
   *
   * @tparam T The type of the body.
   * @param body   The body to set.
   * @param format The format which converts the given format into a body instance.
   * @return A request builder to provide a fluent interface.
   */
  def withBody[T](body: T)(implicit format: BodyFormat[T]): Self = withBody(format.write(body))

  /**
   * Execute the request and produce a response.
   *
   * @return The response.
   */
  def execute: Future[Response]
}
