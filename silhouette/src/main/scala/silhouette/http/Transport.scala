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
package silhouette.http

/**
 * Marker trait for the transport settings.
 */
trait TransportSettings

/**
 * Defines the way how payload can be transported in an HTTP request or an HTTP response.
 *
 * The transport is divided into two parts. The [[RequestTransport]] and the [[ResponseTransport]].
 */
trait Transport {

  /**
   * The type of the concrete implementation of this abstract type.
   */
  type Self

  /**
   * The settings type.
   */
  type Settings <: TransportSettings

  /**
   * The transport settings.
   */
  protected val settings: Settings

  /**
   * Gets a transport initialized with a new settings object.
   *
   * @param f A function which gets the settings passed and returns different settings.
   * @return An instance of the transport initialized with new settings.
   */
  def withSettings(f: Settings => Settings): Self
}

/**
 * The request transport handles payload which can be transported in a request.
 */
trait RequestTransport extends Transport {

  /**
   * Retrieves payload from the given request.
   *
   * @param request The request pipeline to retrieve the payload from.
   * @tparam R The type of the request.
   * @return Some value or None if no payload could be found in request.
   */
  def retrieve[R](request: RequestPipeline[R]): Option[String]

  /**
   * Embeds payload into the request.
   *
   * This method can be used to embed payload in an existing request. This can be useful
   * for testing. Already existing payload will be overridden.
   *
   * @param payload The payload to embed.
   * @param request The request pipeline.
   * @tparam R The type of the request.
   * @return The manipulated request pipeline.
   */
  def embed[R](payload: String, request: RequestPipeline[R]): RequestPipeline[R]
}

/**
 * The response transport handles payload which can be transported in a response.
 */
trait ResponseTransport extends Transport {

  /**
   * Embeds payload into the response.
   *
   * @param payload The payload to embed.
   * @param response The response pipeline to manipulate.
   * @tparam P The type of the response.
   * @return The manipulated response pipeline.
   */
  def embed[P](payload: String, response: ResponsePipeline[P]): ResponsePipeline[P]

  /**
   * Manipulates the response so that it removes payload stored on the client.
   *
   * @param response The response pipeline to manipulate.
   * @tparam P The type of the response.
   * @return The manipulated response pipeline.
   */
  def discard[P](response: ResponsePipeline[P]): ResponsePipeline[P]
}
