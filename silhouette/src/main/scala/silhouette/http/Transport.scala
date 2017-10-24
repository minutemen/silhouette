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

import silhouette.util

import scala.util.Try

/**
 * Transforms a string into an instance of `T`.
 *
 * @tparam T The target type on the read operation.
 */
trait Reads[T] extends util.Reads[String, Try[T]]

/**
 * Transforms an instance of `T` into a string.
 *
 * @tparam T The source type on the write operation
 */
trait Writes[T] extends util.Writes[T, Try[String]]

/**
 * Transport transformer combinator.
 *
 * @tparam T The target type on the read operation and the source type on the write operation.
 */
trait Format[T] extends Reads[T] with Writes[T]

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
trait RequestTransport extends Transport

/**
 * A request transport which can retrieve payload from a request.
 */
trait RetrieveFromRequest extends RequestTransport {

  /**
   * Retrieves payload from the given request.
   *
   * @param request The request pipeline to retrieve the payload from.
   * @tparam R The type of the request.
   * @return Some payload or None if no payload could be found in request.
   */
  def retrieve[R](request: RequestPipeline[R]): Option[String]
}

/**
 * A request transport which can smuggle some payload into a request.
 */
trait SmuggleIntoRequest extends RequestTransport {

  /**
   * Smuggles payload into the request.
   *
   * This method can be used to smuggle payload into an existing request. This can be useful
   * for testing. Already existing payload will be overridden.
   *
   * @param payload The payload to smuggle into the request.
   * @param request The request pipeline.
   * @tparam R The type of the request.
   * @return The manipulated request pipeline.
   */
  def smuggle[R](payload: String, request: RequestPipeline[R]): RequestPipeline[R]
}

/**
 * The response transport handles payload which can be transported in a response.
 */
trait ResponseTransport extends Transport

/**
 * A response transport which can embed payload into a response.
 */
trait EmbedIntoResponse extends ResponseTransport {

  /**
   * Embeds payload into the response.
   *
   * @param payload  The payload to embed into the response.
   * @param response The response pipeline to manipulate.
   * @tparam P The type of the response.
   * @return The manipulated response pipeline.
   */
  def embed[P](payload: String, response: ResponsePipeline[P]): ResponsePipeline[P]
}

/**
 * A response transport which can discard payload from a response.
 */
trait DiscardFromResponse extends ResponseTransport {

  /**
   * Manipulates the response so that it removes payload stored on the client.
   *
   * @param response The response pipeline to manipulate.
   * @tparam P The type of the response.
   * @return The manipulated response pipeline.
   */
  def discard[P](response: ResponsePipeline[P]): ResponsePipeline[P]
}
