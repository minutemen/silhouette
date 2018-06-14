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

import silhouette.http.client.RequestBuilder

/**
 * A client which can be used to process HTTP requests.
 *
 * The concrete implementation of this client must be implemented by the framework specific Silhouette binding.
 * The client is no full-blown HTTP client implementation. It implements only the parts which Silhouette depends on,
 * and it is only meant for internal usage.
 *
 * @tparam B The framework specific [[RequestBuilder]] implementation.
 */
private[silhouette] trait Client[B <: RequestBuilder] {

  /**
   * The framework specific [[RequestBuilder]] implementation.
   *
   * @return The framework specific [[RequestBuilder]] implementation.
   */
  def requestBuilder: B

  /**
   * Sets the URL the client should process and returns the framework specific [[RequestBuilder]] implementation
   * to model the HTTP request.
   *
   * @param url The URL to process.
   * @return The framework specific [[RequestBuilder]] implementation to provide a fluent interface.
   */
  def withUrl(url: String): B#Self = requestBuilder.withUrl(url)
}
