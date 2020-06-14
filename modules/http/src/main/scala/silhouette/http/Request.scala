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
import sttp.model._

/**
 * Represents a request that offers access to the framework specific request implementation.
 */
trait Request extends WithHeaders with WithCookies {

  /**
   * Gets the absolute URI of the request target.
   *
   * This must contain the absolute URI of the request target, because we need this to resolve relative URIs
   * against this.
   *
   * @return The absolute URI of the request target.
   */
  def uri: Uri

  /**
   * Gets the HTTP request method.
   *
   * @return The HTTP request method.
   */
  def method: Method

  /**
   * Gets the raw query string.
   *
   * @return The raw query string.
   */
  def rawQueryString: String = uri.toJavaUri.getRawQuery

  /**
   * Gets all query params.
   *
   * While there is no definitive standard, most web frameworks allow duplicate params with the
   * same name. Therefore we must define a query param values as list of values.
   *
   * @return All query params.
   */
  def queryParams: QueryParams = uri.params

  /**
   * Gets the values for a query param.
   *
   * While there is no definitive standard, most web frameworks allow duplicate params with the
   * same name. Therefore we must define a query param values as list of values.
   *
   * @param name The name of the query param for which the values should be returned.
   * @return A list of param values for the given name or an empty list if no params for the given name could be found.
   */
  def queryParamValues(name: String): Seq[String] = queryParams.getMulti(name).getOrElse(Nil)

  /**
   * Optionally returns the first query param value associated with the given query param name.
   *
   * While there is no definitive standard, most web frameworks allow duplicate params with the
   * same name. Therefore we must define a query param values as list of values.
   *
   * @param name The name of the query param for which the values should be returned.
   * @return Some query param value for the given name, None if no query param for the given name could be found.
   */
  def queryParamValue(name: String): Option[String] = queryParams.get(name)

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
  def fingerprint(): String =
    Hash.sha1(
      new StringBuilder()
        .append(headerValue(HeaderNames.UserAgent).getOrElse(""))
        .append(":")
        .append(headerValue(HeaderNames.AcceptLanguage).getOrElse(""))
        .append(":")
        .append(headerValue(HeaderNames.AcceptCharset).getOrElse(""))
        .toString()
    )

  /**
   * Indicates if the request is a secure HTTPS request.
   *
   * @return True if the request is a secure HTTPS request, false otherwise.
   */
  def isSecure: Boolean = uri.scheme == "https"
}
