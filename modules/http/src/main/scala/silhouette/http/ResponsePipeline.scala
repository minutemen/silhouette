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

/**
 * Decorates a framework specific response implementation.
 *
 * Frameworks should create an implicit conversion between the implementation of this pipeline and
 * the Framework specific response instance.
 *
 * @tparam R The type of the response.
 */
trait ResponsePipeline[R] {

  /**
   * Gets the HTTP status code.
   *
   * @return The HTTP status code.
   */
  def status: Status

  /**
   * Gets all headers.
   *
   * @return All headers.
   */
  def headers: Seq[Header]

  /**
   * Gets the header for the given name.
   *
   * @param name The name of the header for which the header should be returned.
   * @return Some header for the given name, None if no header for the given name could be found.
   */
  def header(name: Header.Name): Option[Header] = headers.find(_.name == name)

  /**
   * Creates a new response pipeline with the given headers.
   *
   * This method must override any existing header with the same name. If multiple headers with the
   * same name are given to this method, then the values must be composed into a list.
   *
   * If a request holds the following headers, then this method must implement the following behaviour:
   * {{{
   *   Seq(
   *     Header("TEST1", Seq("value1", "value2")),
   *     Header("TEST2", "value1")
   *   )
   * }}}
   *
   * Append a new header:
   * {{{
   *   withHeaders(Header("TEST3", "value1"))
   *
   *   Seq(
   *     Header("TEST1" -> Seq("value1", "value2")),
   *     Header("TEST2" -> Seq("value1")),
   *     Header("TEST3" -> Seq("value1"))
   *   )
   * }}}
   *
   * Override the header `TEST1` with a new value:
   * {{{
   *   withHeaders(Header("TEST1", "value3"))
   *
   *   Seq(
   *     Header("TEST1", Seq("value3")),
   *     Header("TEST2", Seq("value1"))
   *   )
   * }}}
   *
   * Compose headers with the same name:
   * {{{
   *   withHeaders(Header("TEST1", "value3"), Header("TEST1", Seq("value4", "value5")))
   *
   *   Set(
   *     Header("TEST1", Seq("value3", "value4", "value5")),
   *     Header("TEST2", Seq("value1"))
   *   )
   * }}}
   *
   * @param headers The headers to set.
   * @return A new response pipeline instance with the set headers.
   */
  def withHeaders(headers: Header*): ResponsePipeline[R]

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
   * Creates a new response pipeline with the given cookies.
   *
   * This method must override any existing cookie with the same name. If multiple cookies with the
   * same name are given to this method, then the last cookie in the list wins.
   *
   * If a response holds the following cookies, then this method must implement the following behaviour:
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
   * @return A new response pipeline instance with the set cookies.
   */
  def withCookies(cookies: Cookie*): ResponsePipeline[R]

  /**
   * Unboxes the framework specific response implementation.
   *
   * @return The framework specific response implementation.
   */
  def unbox: R
}
