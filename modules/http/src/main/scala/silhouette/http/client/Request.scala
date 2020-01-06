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

import java.net.URI

import silhouette.http._

import scala.io.Codec

/**
 * The request an HTTP client handles.
 *
 * @param uri         The request URI.
 * @param method      The HTTP method.
 * @param headers     The HTTP headers.
 * @param queryParams The query params.
 * @param body        The request body.
 */
protected[silhouette] case class Request(
  uri: URI,
  method: Method = Method.GET,
  headers: List[Header] = List(),
  queryParams: List[(String, String)] = List(),
  body: Option[Body] = None
) {

  /**
   * Returns a copy of this instance with a new URI.
   *
   * @param uri The URI to set.
   * @return A request to provide a fluent interface.
   */
  def withUri(uri: URI): Request = copy(uri = uri)

  /**
   * Returns a copy of this instance with a new HTTP method.
   *
   * @param method The HTTP method to set.
   * @return A request to provide a fluent interface.
   */
  def withMethod(method: Method): Request = copy(method = method)

  /**
   * Returns a copy of this instance with the list of headers set.
   *
   * @param headers The headers to set.
   * @return A request to provide a fluent interface.
   */
  def withHeaders(headers: Header*): Request = copy(headers = this.headers ++ headers)

  /**
   * Returns a copy of this instance with the list of query params set.
   *
   * @param params The query params to set.
   * @return A request to provide a fluent interface.
   */
  def withQueryParams(params: (String, String)*): Request = copy(queryParams = this.queryParams ++ params)

  /**
   * Returns a copy of this instance with a new body.
   *
   * @param body The body to set.
   * @return A request to provide a fluent interface.
   */
  def withBody(body: Body): Request = copy(body = Some(body))

  /**
   * Returns a copy of this instance with a new body.
   *
   * @tparam T The type of the body.
   * @param body   The new body.
   * @param codec  The codec of the resulting body.
   * @param writer The body writer that converts the given format into a body instance.
   * @return A request to provide a fluent interface.
   */
  def withBody[T](body: T, codec: Codec = Body.DefaultCodec)(implicit writer: Codec => BodyWriter[T]): Request =
    withBody(writer(codec)(body))
}

/**
 * The companion object.
 */
protected[silhouette] object Request {

  /**
   * Constructs a [[Request]].
   *
   * @param method The HTTP method.
   * @param uri    The request URI.
   */
  def apply(method: Method, uri: URI): Request = Request(uri, method)
}
