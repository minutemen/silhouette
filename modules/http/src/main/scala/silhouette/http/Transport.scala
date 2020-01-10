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

/**
 * Marker trait for the transport config.
 */
trait TransportConfig

/**
 * Defines the way how payload can be transported in an HTTP request or an HTTP response.
 *
 * The transport is divided into two parts. The [[RequestTransport]] and the [[ResponseTransport]].
 */
trait Transport

/**
 * The request transport handles payload which can be transported in a request.
 */
trait RequestTransport extends Transport

/**
 * The response transport handles payload which can be transported in a response.
 */
trait ResponseTransport extends Transport

/**
 * A request transport which can retrieve payload from a request.
 */
trait RetrieveFromRequest extends RequestTransport {

  /**
   * Retrieves payload from the given request.
   *
   * @param request The request to retrieve the payload from.
   * @return Some payload or None if no payload could be found in request.
   */
  def retrieve(request: Request): Option[String]
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
   * Manipulates the response so that it removes payload from it.
   *
   * @param response The response pipeline to manipulate.
   * @tparam P The type of the response.
   * @return The manipulated response pipeline.
   */
  def discard[P](response: ResponsePipeline[P]): ResponsePipeline[P]
}

/**
 * A function that tries to retrieve some payload from the request.
 *
 * @tparam P The type of the payload.
 */
trait Retrieve[P] extends (Request => Option[P])

/**
 * A function that smuggles some payload into the request.
 *
 * @tparam R The type of the request.
 * @tparam P The type of the payload.
 */
trait Smuggle[P, R] extends (P => RequestPipeline[R]) {

  /**
   * The request pipeline into which the payload should be smuggled.
   */
  protected val requestPipeline: RequestPipeline[R]
}

/**
 * A function that embeds some payload into the response.
 *
 * @tparam P The type of the payload.
 * @tparam R The type of the response.
 */
trait Embed[P, R] extends (P => ResponsePipeline[R]) {

  /**
   * The response pipeline into which the payload should be embedded.
   */
  protected val responsePipeline: ResponsePipeline[R]
}

/**
 * A function that discards some payload from the response.
 *
 * @tparam R The type of the response.
 */
trait Discard[R] extends (Unit => ResponsePipeline[R]) {

  /**
   * The response pipeline from which the payload should be discarded.
   */
  protected val responsePipeline: ResponsePipeline[R]
}
