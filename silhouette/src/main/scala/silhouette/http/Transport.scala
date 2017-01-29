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

import silhouette.util.Format

import scala.language.implicitConversions

/**
 * Marker trait for the transport settings.
 */
trait TransportSettings

/**
 * Defines the way how payload can be transported in an HTTP request or an HTTP response.
 *
 * The transport is divided into two parts. The [[RequestTransport]] and the [[ResponseTransport]].
 *
 * @tparam A The internal type every transport implementation handles.
 * @tparam B The type of the payload to transport.
 */
trait Transport[A, B] {

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
   * The format to transform between the transport specific value and the payload.
   */
  protected val format: Format[A, B]

  /**
   * Gets a transport initialized with a new settings object.
   *
   * @param f A function which gets the settings passed and returns different settings.
   * @return An instance of the transport initialized with new settings.
   */
  def withSettings(f: Settings => Settings): Self

  /**
   * Transforms the transport specific value into the payload.
   *
   * @param value The transport specific value to read from.
   * @return The transformed payload or an exception if an error occurred during transformation.
   */
  implicit protected def read(value: A): B = format.read(value).get

  /**
   * Transforms the payload into the transport specific value.
   *
   * @param payload The payload that should be transformed into the transport specific value.
   * @return The transport specific value.
   */
  implicit protected def write(payload: B): A = format.write(payload)
}

/**
 * The request transport handles payload which can be transported in a request.
 *
 * @tparam A The internal type every transport implementation handles.
 * @tparam B The type of the payload to transport.
 */
trait RequestTransport[A, B] extends Transport[A, B] {

  /**
   * Retrieves payload from the given request.
   *
   * @param request The request pipeline to retrieve the payload from.
   * @tparam R The type of the request.
   * @return Some value or None if no payload could be found in request.
   */
  def retrieve[R](request: RequestPipeline[R]): Option[B]

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
  def embed[R](payload: B, request: RequestPipeline[R]): RequestPipeline[R]
}

/**
 * The response transport handles payload which can be transported in a response.
 *
 * @tparam A The internal type every transport implementation handles.
 * @tparam B The type of the payload to transport.
 */
trait ResponseTransport[A, B] extends Transport[A, B] {

  /**
   * Embeds payload into the response.
   *
   * @param payload  The payload to embed.
   * @param response The response pipeline to manipulate.
   * @tparam P The type of the response.
   * @return The manipulated response pipeline.
   */
  def embed[P](payload: B, response: ResponsePipeline[P]): ResponsePipeline[P]

  /**
   * Manipulates the response so that it removes payload stored on the client.
   *
   * @param response The response pipeline to manipulate.
   * @tparam P The type of the response.
   * @return The manipulated response pipeline.
   */
  def discard[P](response: ResponsePipeline[P]): ResponsePipeline[P]
}
