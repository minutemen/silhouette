/**
 * Original work: SecureSocial (https://github.com/jaliss/securesocial)
 * Copyright 2013 Jorge Aliss (jaliss at gmail dot com) - twitter: @jaliss
 *
 * Derivative work: Silhouette (https://github.com/mohiva/silhouette)
 * Modifications Copyright 2015 Mohiva Organisation (license at mohiva dot com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mohiva.silhouette.http

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
   * Gets the list of headers.
   *
   * @return The list of headers.
   */
  def headers: Map[String, Seq[String]]

  /**
   * Gets a header value.
   *
   * @param name The name of the header for which the value should be returned.
   * @return Some header values or None if no header for the given name could be found.
   */
  def header(name: String): Seq[String] = headers.getOrElse(name, Nil)

  /**
   * Sets a list of headers.
   *
   * This method must override any existing header with the same name.
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
   * Sets a list of cookies.
   *
   * This method must override any existing cookie with the same name.
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
   * Sets session data.
   *
   * This method must override any existing session data with the same name.
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
   * Gets a query param from request.
   *
   * @param name The name of the query param to return.
   * @return Some query param or None if no query param for the given name could be found.
   */
  def queryParam(name: String): Option[String]

  /**
   * Unboxes the framework specific request implementation.
   *
   * @return The framework specific request implementation.
   */
  def unbox: R
}
