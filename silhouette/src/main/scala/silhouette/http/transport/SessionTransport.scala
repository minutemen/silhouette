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
 * @param key The session key in which the payload will be transported.
 */
final case class SessionTransportSettings(key: String) extends TransportSettings

/**
 * The session transport.
 *
 * @param settings The transport settings.
 * @param format   The format to transform between the transport specific value and the payload.
 */
final case class SessionTransport[B](settings: SessionTransportSettings)(
  implicit
  protected val format: Format[String, B]
) extends RequestTransport[String, B] with ResponseTransport[String, B] {

  /**
   * The type of the concrete implementation of this abstract type.
   */
  override type Self = SessionTransport[B]

  /**
   * The settings type.
   */
  override type Settings = SessionTransportSettings

  /**
   * Gets a transport initialized with a new settings object.
   *
   * @param f A function which gets the settings passed and returns different settings.
   * @return An instance of the transport initialized with new settings.
   */
  override def withSettings(f: (Settings) => Settings): Self = new Self(f(settings))

  /**
   * Retrieves the payload, stored in the session, from request.
   *
   * @param request The request pipeline to retrieve the payload from.
   * @tparam R The type of the request.
   * @return Some value or None if no payload could be found in request.
   */
  override def retrieve[R](request: RequestPipeline[R]): Option[B] =
    request.session.get(settings.key).map(read)

  /**
   * Adds a session key with the given payload to the request.
   *
   * @param payload The payload to embed.
   * @param request The request pipeline.
   * @tparam R The type of the request.
   * @return The manipulated request pipeline.
   */
  override def embed[R](payload: B, request: RequestPipeline[R]): RequestPipeline[R] =
    request.withSession(settings.key -> payload)

  /**
   * Adds a session key with the given payload to the response.
   *
   * @param payload  The payload to embed.
   * @param response The response pipeline to manipulate.
   * @tparam P The type of the response.
   * @return The manipulated response pipeline.
   */
  override def embed[P](payload: B, response: ResponsePipeline[P]): ResponsePipeline[P] =
    response.withSession(settings.key -> payload)

  /**
   * Discards the session key on the client.
   *
   * @param response The response pipeline to manipulate.
   * @tparam P The type of the response.
   * @return The manipulated response pipeline.
   */
  override def discard[P](response: ResponsePipeline[P]): ResponsePipeline[P] =
    response.withoutSession(settings.key)
}
