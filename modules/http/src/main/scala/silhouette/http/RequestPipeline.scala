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

import sttp.model.{ CookieWithMeta, Header, Method, Uri }

/**
 * Allows to modify a framework specific request implementation.
 *
 * @tparam R The type of the request.
 */
trait RequestPipeline[+R] extends Request with RequestExtractor[R] {

  /**
   * The type of the request body.
   */
  type RB

  /**
   * The framework specific request implementation.
   */
  protected val request: R

  /**
   * The framework specific request body.
   */
  protected val requestBody: RB

  /**
   * Creates a new request pipeline with the given URI.
   *
   * This must contain the absolute URI of thr request target, because we need this to resolve relative URIs
   * against this URI.
   *
   * @param uri The absolute URI of the request target.
   * @return A new request pipeline instance with the set URI.
   */
  def withUri(uri: Uri): RequestPipeline[R]

  /**
   * Creates a new request pipeline with the given HTTP request method.
   *
   * @param method The HTTP request method to set.
   * @return A new request pipeline instance with the set HTTP request method.
   */
  def withMethod(method: Method): RequestPipeline[R]

  /**
   * Creates a new request pipeline with the given headers.
   *
   * This method appends new headers to the existing list of headers. Already existing headers with the same name will
   * be kept untouched.
   *
   * If a request holds the following headers, then this method must implement the following behaviour:
   * {{{
   *   Seq(
   *     Header("TEST1", "value1, value2"),
   *     Header("TEST2", "value1")
   *   )
   * }}}
   *
   * Append a new headers:
   * {{{
   *   withHeaders(Header("TEST3", "value1"), Header("TEST1", "value3"), Header("TEST1", "value4, value5"))
   *
   *   Seq(
   *     Header("TEST1", "value1, value2"),
   *     Header("TEST2", "value1"),
   *     Header("TEST3", "value1"),
   *     Header("TEST1", "value3"),
   *     Header("TEST1", "value4, value5")
   *   )
   * }}}
   *
   * @param headers The headers to set.
   * @return A new request pipeline instance with the set headers.
   */
  def withHeaders(headers: Header*): RequestPipeline[R]

  /**
   * Creates a new request pipeline with the given cookies.
   *
   * This method replaces any existing cookie with the same name. If multiple cookies with the
   * same name are given to this method, then the last cookie in the list wins.
   *
   * If a request holds the following cookies, then this method must implement the following behaviour:
   * {{{
   *   Seq(
   *     Cookie("test1", "value1"),
   *     Cookie("test2", "value2")
   *   )
   * }}}
   *
   * Append a new cookie:
   * {{{
   *   withCookies(Cookie("test3", "value3"))
   *
   *   Seq(
   *     Cookie("test1", "value1"),
   *     Cookie("test2", "value2"),
   *     Cookie("test3", "value3")
   *   )
   * }}}
   *
   * Override the cookie `test1`:
   * {{{
   *   withCookies(Cookie("test1", "value3"))
   *
   *   Seq(
   *     Cookie("test1", "value3"),
   *     Cookie("test2", "value2")
   *   )
   * }}}
   *
   * Use the last cookie if multiple cookies with the same name are given:
   * {{{
   *   withCookies(Cookie("test1", "value3"), Cookie("test1", "value4"))
   *
   *   Seq(
   *     Cookie("test1", "value4"),
   *     Cookie("test2", "value2")
   *   )
   * }}}
   *
   * @param cookies The cookies to set.
   * @return A new request pipeline instance with the set cookies.
   */
  def withCookies(cookies: CookieWithMeta*): RequestPipeline[R]

  /**
   * Creates a new request pipeline with the given query params.
   *
   * This method appends new query params to the existing list of query params. Already existing params with the same
   * name will be kept untouched.
   *
   * If a request holds the following query params, then this method must implement the following behaviour:
   * {{{
   *   Seq(
   *     ("test1", Seq("value1", "value2")),
   *     ("test2", Seq("value2"))
   *   )
   * }}}
   *
   * Append a new query param:
   * {{{
   *   withQueryParams("test3" -> "value1", "test1" -> "value3", "test1" -> "value4"))
   *
   *   Map(
   *     "test1" -> Seq("value1", "value2"),
   *     "test2" -> Seq("value1"),
   *     "test3" -> Seq("value1"),
   *     "test1" -> Seq("value3"),
   *     "test1" -> Seq("value4")
   *   )
   * }}}
   *
   * @param params The query params to set.
   * @return A new request pipeline instance with the set query params.
   */
  def withQueryParams(params: (String, String)*): RequestPipeline[R]

  /**
   * Creates a new request pipeline with the given body extractor.
   *
   * @param bodyExtractor The body extractor to set.
   * @return A new request pipeline instance with the set body extractor.
   */
  def withBodyExtractor(bodyExtractor: RequestBodyExtractor[RB]): RequestPipeline[R]

  /**
   * Generates a fingerprint from request.
   *
   * @param generator A generator function to create a fingerprint from request.
   * @return A fingerprint of the client.
   */
  def fingerprint(generator: RequestPipeline[R] => String): String = generator(this)

  /**
   * Unboxes the framework specific request implementation.
   *
   * @return The framework specific request implementation.
   */
  def unbox: R = request
}
