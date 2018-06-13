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

/**
 * The response an HTTP client returns after a request.
 */
private[silhouette] trait Response {

  /**
   * Gets all headers.
   *
   * The HTTP RFC2616 allows duplicate response headers with the same name. Therefore we must define a
   * header values as sequence of values.
   *
   * @see https://www.w3.org/Protocols/rfc2616/rfc2616-sec4.html#sec4.2
   *
   * @return All headers.
   */
  def header: Map[String, Seq[String]]

  /**
   * Returns the HTTP status code of this response.
   *
   * @return The HTTP status code.
   */
  def status: Int

  /**
   * Returns the response body.
   *
   * @return The response body.
   */
  def body: Body
}
