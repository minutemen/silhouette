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

import sttp.model.{ Header, Headers }

/**
 * A trait that provides methods to access headers.
 */
trait WithHeaders {

  /**
   * Gets all headers.
   *
   * @return All headers.
   */
  def headers: Headers

  /**
   * Retrieve all headers associated with the given header name.
   *
   * @param name The name of the header for which the headers should be returned.
   * @return The list of headers for the given name or an empty list if no headers for the given name could
   *         be found.
   */
  def headers(name: String): Seq[Header] = headers.headers.filter(_.is(name))

  /**
   * Optionally returns the first header associated with the given header name.
   *
   * @param name The name of the header for which the header should be returned.
   * @return Some header for the given name, None if no header for the given name could be found.
   */
  def header(name: String): Option[Header] = headers.headers.find(_.is(name))

  /**
   * Retrieve all header values associated with the given header name.
   *
   * @param name The name of the header for which the header values should be returned.
   * @return The list of header values for the given name or an empty list if no headers for the given name could
   *         be found.
   */
  def headerValues(name: String): Seq[String] = headers.headers(name)

  /**
   * Optionally returns the first header value associated with the given header name.
   *
   * @param name The name of the header for which the header value should be returned.
   * @return Some header value for the given name, None if no header for the given name could be found.
   */
  def headerValue(name: String): Option[String] = headers.header(name)
}
