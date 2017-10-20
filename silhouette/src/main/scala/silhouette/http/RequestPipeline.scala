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

import silhouette.crypto.Hash
import silhouette.crypto.Hash._

/**
 * Decorates a framework specific request implementation.
 *
 * Frameworks should create an implicit conversion between the implementation of this pipeline and
 * the Framework specific request instance.
 *
 * @tparam R The type of the request.
 */
protected[silhouette] trait RequestPipeline[R] extends RequestExtractor[R] {

  /**
   * The framework specific request implementation.
   */
  val request: R

  /**
   * Gets all headers.
   *
   * The HTTP RFC2616 allows duplicate request headers with the same name. Therefore we must define a
   * header values as sequence of values.
   *
   * @see https://www.w3.org/Protocols/rfc2616/rfc2616-sec4.html#sec4.2
   *
   * @return All headers.
   */
  def headers: Map[String, Seq[String]]

  /**
   * Gets the values for a header.
   *
   * The HTTP RFC2616 allows duplicate request headers with the same name. Therefore we must define a
   * header values as sequence of values.
   *
   * @see https://www.w3.org/Protocols/rfc2616/rfc2616-sec4.html#sec4.2
   *
   * @param name The name of the header for which the values should be returned.
   * @return A list of header values for the given name or an empty list if no header for the given name could be found.
   */
  def header(name: String): Seq[String] = headers.getOrElse(name, Nil)

  /**
   * Creates a new request pipeline with the given headers.
   *
   * This method must override any existing header with the same name. If multiple headers with the
   * same name are given to this method, then the values must be composed into a list.
   *
   * If a request holds the following headers, then this method must implement the following behaviour:
   * {{{
   *   Map(
   *     "TEST1" -> Seq("value1", "value2"),
   *     "TEST2" -> Seq("value1")
   *   )
   * }}}
   *
   * Append a new header:
   * {{{
   *   withHeaders("TEST3" -> "value1")
   *
   *   Map(
   *     "TEST1" -> Seq("value1", "value2"),
   *     "TEST2" -> Seq("value1"),
   *     "TEST3" -> Seq("value1")
   *   )
   * }}}
   *
   * Override the header `TEST1` with a new value:
   * {{{
   *   withHeaders("TEST1" -> "value3")
   *
   *   Map(
   *     "TEST1" -> Seq("value3"),
   *     "TEST2" -> Seq("value1")
   *   )
   * }}}
   *
   * Compose headers with the same name:
   * {{{
   *   withHeaders("TEST1" -> "value3", "TEST1" -> "value4")
   *
   *   Map(
   *     "TEST1" -> Seq("value3", "value4"),
   *     "TEST2" -> Seq("value1")
   *   )
   * }}}
   *
   * @param headers The headers to set.
   * @return A new request pipeline instance with the set headers.
   */
  def withHeaders(headers: (String, String)*): RequestPipeline[R]

  /**
   * Gets the list of cookies.
   *
   * @return The list of cookies.
   */
  def cookies: Seq[Cookie]

  /**
   * Gets a cookie.
   *
   * @param name The name for which the cookie should be returned.
   * @return Some cookie or None if no cookie for the given name could be found.
   */
  def cookie(name: String): Option[Cookie] = cookies.find(_.name == name)

  /**
   * Creates a new request pipeline with the given cookies.
   *
   * This method must override any existing cookie with the same name. If multiple cookies with the
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
  def withCookies(cookies: Cookie*): RequestPipeline[R]

  /**
   * Gets the session data.
   *
   * @return The session data.
   */
  def session: Map[String, String]

  /**
   * Creates a new request pipeline with the given session data.
   *
   * This method must override any existing session data with the same name. If multiple session data with the
   * same key are given to this method, then the last session data in the list wins.
   *
   * If a request holds the following session data, then this method must implement the following behaviour:
   * {{{
   *   Map(
   *     "test1" -> "value1",
   *     "test2" -> "value2"
   *   )
   * }}}
   *
   * Append new session data:
   * {{{
   *   withSession("test3" -> "value3")
   *
   *   Map(
   *     "test1" -> "value1",
   *     "test2" -> "value2",
   *     "test3" -> "value3"
   *   )
   * }}}
   *
   * Override the session data with the key `test1`:
   * {{{
   *   withSession("test1" -> "value3")
   *
   *   Map(
   *     "test1" -> "value3",
   *     "test2" -> "value2"
   *   )
   * }}}
   *
   * Use the last session data if multiple session data with the same key are given:
   * {{{
   *   withSession("test1" -> "value3", "test1" -> "value4")
   *
   *   Map(
   *     "test1" -> "value4",
   *     "test2" -> "value2"
   *   )
   * }}}
   *
   * @param data The session data to set.
   * @return A new request pipeline instance with the set session data.
   */
  def withSession(data: (String, String)*): RequestPipeline[R]

  /**
   * Gets the raw query string.
   *
   * @return The raw query string.
   */
  def rawQueryString: String

  /**
   * Gets all query params.
   *
   * While there is no definitive standard, most web frameworks allow duplicate params with the
   * same name. Therefore we must define a query param values as sequence of values.
   *
   * @return All query params.
   */
  def queryParams: Map[String, Seq[String]]

  /**
   * Gets the values for a query param.
   *
   * While there is no definitive standard, most web frameworks allow duplicate params with the
   * same name. Therefore we must define a query param values as sequence of values.
   *
   * @param name The name of the query param for which the values should be returned.
   * @return A list of param values for the given name or an empty list if no params for the given name could be found.
   */
  def queryParam(name: String): Seq[String] = queryParams.getOrElse(name, Nil)

  /**
   * Creates a new request pipeline with the given query params.
   *
   * This method must override any existing query param with the same name. If multiple query params with the
   * same name are given to this method, then the values must be composed into a list.
   *
   * If a request holds the following query params, then this method must implement the following behaviour:
   * {{{
   *   Map(
   *     "TEST1" -> Seq("value1", "value2"),
   *     "TEST2" -> Seq("value1")
   *   )
   * }}}
   *
   * Append a new query param:
   * {{{
   *   withQueryParams("test3" -> "value1")
   *
   *   Map(
   *     "test1" -> Seq("value1", "value2"),
   *     "test2" -> Seq("value1"),
   *     "test3" -> Seq("value1")
   *   )
   * }}}
   *
   * Override the query param `test1` with a new value:
   * {{{
   *   withQueryParams("test1" -> "value3")
   *
   *   Map(
   *     "test1" -> Seq("value3"),
   *     "test2" -> Seq("value1")
   *   )
   * }}}
   *
   * Compose query params with the same name:
   * {{{
   *   withQueryParams("test1" -> "value3", "test1" -> "value4")
   *
   *   Map(
   *     "test1" -> Seq("value3", "value4"),
   *     "test2" -> Seq("value1")
   *   )
   * }}}
   *
   * @param params The query params to set.
   * @return A new request pipeline instance with the set query params.
   */
  def withQueryParams(params: (String, String)*): RequestPipeline[R]

  /**
   * Generates a default fingerprint from common request headers.
   *
   * A generator which creates a SHA1 fingerprint from `User-Agent`, `Accept-Language` and `Accept-Charset` headers.
   *
   * The `Accept` header would also be a good candidate, but this header makes problems in applications
   * which uses content negotiation. So the default fingerprint generator doesn't include it.
   *
   * The same with `Accept-Encoding`. But in Chromium/Blink based browser the content of this header may
   * be changed during requests.
   *
   * @return A default fingerprint from the request.
   */
  def fingerprint: String = {
    Hash.sha1(new StringBuilder()
      .append(headers.getOrElse("User-Agent", "")).append(":")
      .append(headers.getOrElse("Accept-Language", "")).append(":")
      .append(headers.getOrElse("Accept-Charset", "")).append(":")
      .toString()
    )
  }

  /**
   * Generates a fingerprint from request.
   *
   * @param generator A generator function to create a fingerprint from request.
   * @return A fingerprint of the client.
   */
  def fingerprint(generator: R => String): String = generator(request)

  /**
   * Unboxes the framework specific request implementation.
   *
   * @return The framework specific request implementation.
   */
  def unbox: R
}
