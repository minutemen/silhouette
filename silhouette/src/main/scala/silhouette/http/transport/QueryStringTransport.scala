/**
 * Copyright 2016 Mohiva Organisation (license at mohiva dot com)
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
package silhouette.http.transport

import silhouette.http.{ RequestPipeline, RequestTransport, TransportSettings }

/**
 * The settings for the header transport.
 *
 * @param name The name of the query param in which the payload will be transported.
 */
final case class QueryStringTransportSettings(name: String) extends TransportSettings

/**
 * The query string transport.
 *
 * A query string does only exists for a request. So this is the only implementation.
 *
 * @param settings The transport settings.
 */
final case class QueryStringRequestTransport(settings: QueryStringTransportSettings) extends RequestTransport {

  /**
   * The type of the concrete implementation of this abstract type.
   */
  override type Self = QueryStringRequestTransport

  /**
   * The settings type.
   */
  override type Settings = QueryStringTransportSettings

  /**
   * Gets a transport initialized with a new settings object.
   *
   * @param f A function which gets the settings passed and returns different settings.
   * @return An instance of the transport initialized with new settings.
   */
  override def withSettings(f: (Settings) => Settings) = new Self(f(settings))

  /**
   * Retrieves the payload, stored in the query string, from request.
   *
   * @param request The request pipeline to retrieve the payload from.
   * @tparam R The type of the request.
   * @return Some value or None if no payload could be found in request.
   */
  override def retrieve[R](request: RequestPipeline[R]): Option[String] =
    request.queryParam(settings.name).headOption

  /**
   * Adds a query param with the given payload to the request.
   *
   * @param payload The payload to embed.
   * @param request The request pipeline.
   * @tparam R The type of the request.
   * @return The manipulated request pipeline.
   */
  override def embed[R](payload: String, request: RequestPipeline[R]): RequestPipeline[R] =
    request.withQueryParams(settings.name -> payload)
}
