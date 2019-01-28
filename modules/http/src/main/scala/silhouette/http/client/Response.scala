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

import silhouette.http.{ Body, Header, Status }

/**
 * The response an HTTP client returns after a request.
 *
 * The HTTP RFC2616 allows duplicate response headers with the same name. Therefore we must define a
 * header values as sequence of values.
 *
 * https://www.w3.org/Protocols/rfc2616/rfc2616-sec4.html#sec4.2
 *
 * @param status  The HTTP status.
 * @param headers The HTTP response headers.
 * @param body    An optional HTTP body.
 */
private[silhouette] class Response(
  val status: Status,
  val headers: List[Header] = List(),
  val body: Option[Body] = None
)

/**
 * The companion object.
 */
object Response {

  /**
   * Creates a new response.
   *
   * @param status  The HTTP status.
   * @return A response.
   */
  def apply(status: Status): Response = new Response(status)

  /**
   * Creates a new response.
   *
   * @param status  The HTTP status.
   * @param body    The response body.
   * @return A response.
   */
  def apply(status: Status, body: Body): Response = new Response(status, body = Some(body))

  /**
   * Creates a new response.
   *
   * @param status  The HTTP status.
   * @param headers The HTTP response headers.
   * @return A response.
   */
  def apply(status: Status, headers: List[Header]): Response = new Response(status, headers)

  /**
   * Creates a new response.
   *
   * @param status  The HTTP status.
   * @param body    The response body.
   * @param headers The HTTP response headers.
   * @return A response.
   */
  def apply(status: Status, body: Body, headers: List[Header]): Response = new Response(status, headers, Some(body))
}
