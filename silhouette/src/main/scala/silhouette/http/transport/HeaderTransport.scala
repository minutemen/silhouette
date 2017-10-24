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
import silhouette.util.{ Source, Target }

/**
 * The settings for the header transport.
 *
 * @param name The name of the header in which the payload will be transported.
 */
final case class HeaderTransportSettings(name: String) extends TransportSettings

/**
 * The header transport.
 *
 * @param settings The transport settings.
 */
final case class HeaderTransport(settings: HeaderTransportSettings)
  extends RetrieveFromRequest
  with SmuggleIntoRequest
  with EmbedIntoResponse {

  /**
   * The type of the concrete implementation of this abstract type.
   */
  override type Self = HeaderTransport

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
   * @return Some payload or None if no payload could be found in request.
   */
  override def retrieve[R](request: RequestPipeline[R]): Option[String] =
    request.header(settings.name).headOption

  /**
   * Adds a header with the given payload to the request.
   *
   * @param payload The payload to smuggle into the request.
   * @param request The request pipeline.
   * @tparam R The type of the request.
   * @return The manipulated request pipeline.
   */
  override def smuggle[R](payload: String, request: RequestPipeline[R]): RequestPipeline[R] =
    request.withHeaders(settings.name -> payload)

  /**
   * Adds a header with the given payload to the response.
   *
   * @param payload  The payload to embed into the response.
   * @param response The response pipeline to manipulate.
   * @tparam R The type of the response.
   * @return The manipulated response pipeline.
   */
  override def embed[R](payload: String, response: ResponsePipeline[R]): ResponsePipeline[R] =
    response.withHeaders(settings.name -> payload)
}

/**
 * A source that tries to retrieve some payload, stored in a header, from the given request.
 *
 * @param name             The name of the header in which the payload will be transported.
 * @param requestPipeline  The request pipeline.
 * @tparam R The type of the request.
 */
case class RetrieveFromHeader[R](name: String)(
  implicit
  requestPipeline: RequestPipeline[R]
) extends Source[Option[String]] {

  /**
   * Retrieves payload from a header.
   *
   * @return The retrieved payload.
   */
  override def read: Option[String] = HeaderTransport(HeaderTransportSettings(name)).retrieve(requestPipeline)
}

/**
 * A target that smuggles a header with the given payload into the given request.
 *
 * @param payload          The payload to embed.
 * @param name             The name of the header in which the payload will be transported.
 * @param requestPipeline  The request pipeline.
 * @tparam R The type of the response.
 */
final case class SmuggleIntoHeader[R](payload: String, name: String)(
  implicit
  requestPipeline: RequestPipeline[R]
) extends Target[RequestPipeline[R]] {

  /**
   * Smuggles payload into a header.
   *
   * @return The request pipeline.
   */
  override def write: RequestPipeline[R] =
    HeaderTransport(HeaderTransportSettings(name)).smuggle(payload, requestPipeline)
}

/**
 * A target that embeds a header with the given payload into the given response.
 *
 * @param payload          The payload to embed.
 * @param name             The name of the header in which the payload will be transported.
 * @param responsePipeline The response pipeline.
 * @tparam R The type of the response.
 */
case class EmbedIntoHeader[R](payload: String, name: String)(
  implicit
  responsePipeline: ResponsePipeline[R]
) extends Target[ResponsePipeline[R]] {

  /**
   * Embeds payload into a header.
   *
   * @return The response pipeline.
   */
  override def write: ResponsePipeline[R] =
    HeaderTransport(HeaderTransportSettings(name)).embed(payload, responsePipeline)
}
