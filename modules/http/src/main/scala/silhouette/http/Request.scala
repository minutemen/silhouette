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

import java.net.{ URI, URLEncoder }

import silhouette.crypto.Hash
import silhouette.crypto.Hash._

/**
 * Represents a request that offers access to the framework specific request implementation.
 */
trait Request {

  /**
   * Gets the absolute URI of the request target.
   *
   * This must contain the absolute URI of thr request target, because we need this to resolve relative URIs
   * against this.
   *
   * @return The absolute URI of the request target.
   */
  def uri: URI

  /**
   * Gets the HTTP request method.
   *
   * @return The HTTP request method.
   */
  def method: Method

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
   * Gets the header value for the given name.
   *
   * @param name The name of the header for which the header value should be returned.
   * @return Some header value for the given name, None if no header for the given name could be found.
   */
  def headerValue(name: Header.Name): Option[String] = header(name).map(_.value)

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
   * Gets the raw query string.
   *
   * @return The raw query string.
   */
  def rawQueryString: String = {
    queryParams.foldLeft(List[String]()) {
      case (acc, (key, value)) =>
        acc :+ value.map(URLEncoder.encode(key, "UTF-8") + "=" + URLEncoder.encode(_, "UTF-8")).mkString("&")
    }.mkString("&")
  }

  /**
   * Gets all query params.
   *
   * While there is no definitive standard, most web frameworks allow duplicate params with the
   * same name. Therefore we must define a query param values as list of values.
   *
   * @return All query params.
   */
  def queryParams: Map[String, Seq[String]]

  /**
   * Gets the values for a query param.
   *
   * While there is no definitive standard, most web frameworks allow duplicate params with the
   * same name. Therefore we must define a query param values as list of values.
   *
   * @param name The name of the query param for which the values should be returned.
   * @return A list of param values for the given name or an empty list if no params for the given name could be found.
   */
  def queryParam(name: String): Seq[String] = queryParams.getOrElse(name, Nil)

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
  def fingerprint(): String = {
    Hash.sha1(new StringBuilder()
      .append(headerValue(Header.Name.`User-Agent`).getOrElse("")).append(":")
      .append(headerValue(Header.Name.`Accept-Language`).getOrElse("")).append(":")
      .append(headerValue(Header.Name.`Accept-Charset`).getOrElse(""))
      .toString()
    )
  }

  /**
   * Indicates if the request is a secure HTTPS request.
   *
   * @return True if the request is a secure HTTPS request, false otherwise.
   */
  def isSecure: Boolean = uri.getScheme == "https"
}
