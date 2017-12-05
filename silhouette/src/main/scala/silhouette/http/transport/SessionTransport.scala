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

/**
 * The session transport.
 *
 * @param key The session key in which the payload will be transported.
 */
final case class SessionTransport(key: String)
  extends RetrieveFromRequest
  with SmuggleIntoRequest
  with EmbedIntoResponse
  with DiscardFromResponse {

  /**
   * Retrieves the payload, stored in the session, from request.
   *
   * @param request The request pipeline to retrieve the payload from.
   * @tparam R The type of the request.
   * @return Some value or None if no payload could be found in request.
   */
  override def retrieve[R](request: RequestPipeline[R]): Option[String] =
    request.session.get(key)

  /**
   * Adds a session key with the given payload to the request.
   *
   * @param payload The payload to smuggle into the request.
   * @param request The request pipeline.
   * @tparam R The type of the request.
   * @return The manipulated request pipeline.
   */
  override def smuggle[R](payload: String, request: RequestPipeline[R]): RequestPipeline[R] =
    request.withSession(key -> payload)

  /**
   * Adds a session key with the given payload to the response.
   *
   * @param payload  The payload to embed into the response.
   * @param response The response pipeline to manipulate.
   * @tparam R The type of the response.
   * @return The manipulated response pipeline.
   */
  override def embed[R](payload: String, response: ResponsePipeline[R]): ResponsePipeline[R] =
    response.withSession(key -> payload)

  /**
   * Discards the session key on the client.
   *
   * @param response The response pipeline to manipulate.
   * @tparam R The type of the response.
   * @return The manipulated response pipeline.
   */
  override def discard[R](response: ResponsePipeline[R]): ResponsePipeline[R] =
    response.withoutSession(key)
}

/**
 * A reads that tries to retrieve some payload, stored in a session key, from the given request.
 *
 * @param key The session key in which the payload will be transported.
 * @tparam R The type of the request.
 */
final case class RetrieveFromSession[R](key: String) extends RetrieveReads[R, String] {

  /**
   * Reads payload from a session key stored in the given request.
   *
   * @param requestPipeline The request pipeline.
   * @return The retrieved payload.
   */
  override def read(requestPipeline: RequestPipeline[R]): Option[String] =
    SessionTransport(key).retrieve(requestPipeline)
}

/**
 * A writes that smuggles a session key with the given payload into the given request.
 *
 * @param key The session key in which the payload will be transported.
 * @tparam R The type of the response.
 */
final case class SmuggleIntoSession[R](key: String) extends SmuggleWrites[R, String] {

  /**
   * Merges some payload and a [[RequestPipeline]] into a [[RequestPipeline]] that contains a session key with the
   * given payload as value.
   *
   * @param in A tuple consisting of the payload to embed in the session and the [[RequestPipeline]] in which the
   *           session key should be embedded.
   * @return The request pipeline with the smuggled session key.
   */
  override def write(in: (String, RequestPipeline[R])): RequestPipeline[R] =
    SessionTransport(key).smuggle[R] _ tupled in
}

/**
 * A writes that embeds a session key with the given payload into the given response.
 *
 * @param key The session key in which the payload will be transported.
 * @tparam R The type of the response.
 */
final case class EmbedIntoSession[R](key: String) extends EmbedWrites[R, String] {

  /**
   * Merges some payload and a [[ResponsePipeline]] into a [[ResponsePipeline]] that contains a session key with the
   * given payload as value.
   *
   * @param in A tuple consisting of the payload to embed in the session and the [[ResponsePipeline]] in which the
   *           session key should be embedded.
   * @return The response pipeline with the embedded session key.
   */
  override def write(in: (String, ResponsePipeline[R])): ResponsePipeline[R] =
    SessionTransport(key).embed[R] _ tupled in
}

/**
 * A writes that discards payload stored in the session from the given response.
 *
 * @param key The session key from which the payload should be discarded.
 * @tparam R The type of the response.
 */
final case class DiscardFromSession[R](key: String) extends DiscardWrites[R] {

  /**
   * Takes a [[ResponsePipeline]] and discards the payload stored in the given session key.
   *
   * @param responsePipeline The response pipeline from which the session key should be discarded.
   * @return The response pipeline without the given session key.
   */
  override def write(responsePipeline: ResponsePipeline[R]): ResponsePipeline[R] =
    SessionTransport(key).discard(responsePipeline)
}
