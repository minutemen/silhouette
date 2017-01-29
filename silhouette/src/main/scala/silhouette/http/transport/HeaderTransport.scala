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
package silhouette.http.transport

import silhouette.http._
import silhouette.util.Format

/**
 * The settings for the header transport.
 *
 * @param name The name of the header in which the payload will be transported.
 */
final case class HeaderTransportSettings(name: String) extends TransportSettings

/**
 * The header transport.
 *
 * @tparam B The type of the payload.
 * @param settings The transport settings.
 * @param format   The format to transform between the transport specific value and the payload.
 */
final case class HeaderTransport[B](settings: HeaderTransportSettings)(
  implicit
  protected val format: Format[String, B]
) extends RequestTransport[String, B] with ResponseTransport[String, B] {

  /**
   * The type of the concrete implementation of this abstract type.
   */
  override type Self = HeaderTransport[B]

  /**
   * The settings type.
   */
  override type Settings = HeaderTransportSettings

  /**
   * Gets a transport initialized with a new settings object.
   *
   * @param f A function which gets the settings passed and returns different settings.
   * @return An instance of the transport initialized with new settings.
   */
  override def withSettings(f: (Settings) => Settings): Self = new Self(f(settings))

  /**
   * Retrieves the payload, stored in a header, from request.
   *
   * @param request The request pipeline to retrieve the payload from.
   * @tparam R The type of the request.
   * @return Some value or None if no payload could be found in request.
   */
  override def retrieve[R](request: RequestPipeline[R]): Option[B] =
    request.header(settings.name).headOption.map(read)

  /**
   * Adds a header with the given payload to the request.
   *
   * @param payload The payload to embed.
   * @param request The request pipeline.
   * @tparam R The type of the request.
   * @return The manipulated request pipeline.
   */
  override def embed[R](payload: B, request: RequestPipeline[R]): RequestPipeline[R] =
    request.withHeaders(settings.name -> payload)

  /**
   * Adds a header with the given payload to the response.
   *
   * @param payload  The payload to embed.
   * @param response The response pipeline to manipulate.
   * @tparam P The type of the response.
   * @return The manipulated response pipeline.
   */
  override def embed[P](payload: B, response: ResponsePipeline[P]): ResponsePipeline[P] =
    response.withHeaders(settings.name -> payload)

  /**
   * Headers needn't be removed from response, because they are not persisted on the client. So this
   * method returns the unchanged response.
   *
   * @param response The response pipeline to manipulate.
   * @tparam P The type of the response.
   * @return The manipulated response pipeline.
   */
  override def discard[P](response: ResponsePipeline[P]): ResponsePipeline[P] = response
}
