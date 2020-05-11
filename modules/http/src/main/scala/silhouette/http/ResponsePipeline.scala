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

import sttp.model.{ CookieWithMeta, Header }

/**
 * Decorates a framework specific response implementation.
 *
 * Frameworks should create an implicit conversion between the implementation of this pipeline and
 * the Framework specific response instance.
 *
 * @tparam R The type of the response.
 */
trait ResponsePipeline[R] extends Response {

  /**
   * Creates a new response pipeline with the given headers.
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
   * @return A new response pipeline instance with the set headers.
   */
  def withHeaders(headers: Header*): ResponsePipeline[R]

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
  def withCookies(cookies: CookieWithMeta*): ResponsePipeline[R]

  /**
   * Unboxes the framework specific response implementation.
   *
   * @return The framework specific response implementation.
   */
  def unbox: R
}
