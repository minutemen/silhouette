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
 * A source that tries to retrieve some payload, stored in a session key, from the given request.
 *
 * @param key             The session key in which the payload will be transported.
 * @param requestPipeline The request pipeline.
 * @tparam R The type of the request.
 */
final case class RetrieveFromSession[R](key: String)(
  implicit
  requestPipeline: RequestPipeline[R]
) extends Source[Option[String]] {

  /**
   * Retrieves payload from a session.
   *
   * @return The retrieved payload.
   */
  override def read: Option[String] = SessionTransport(key).retrieve(requestPipeline)
}

/**
 * A target that smuggles a session key with the given payload into the given request.
 *
 * @param payload          The payload to embed.
 * @param key              The session key in which the payload will be transported.
 * @param requestPipeline  The request pipeline.
 * @tparam R The type of the response.
 */
final case class SmuggleIntoSession[R](payload: String, key: String)(
  implicit
  requestPipeline: RequestPipeline[R]
) extends Target[RequestPipeline[R]] {

  /**
   * Smuggles payload into a session.
   *
   * @return The request pipeline.
   */
  override def write: RequestPipeline[R] = SessionTransport(key).smuggle(payload, requestPipeline)
}

/**
 * A target that embeds a session key with the given payload into the given response.
 *
 * @param payload          The payload to embed.
 * @param key              The session key in which the payload will be transported.
 * @param responsePipeline The response pipeline.
 * @tparam R The type of the response.
 */
final case class EmbedIntoSession[R](payload: String, key: String)(
  implicit
  responsePipeline: ResponsePipeline[R]
) extends Target[ResponsePipeline[R]] {

  /**
   * Embeds payload into a session.
   *
   * @return The response pipeline.
   */
  override def write: ResponsePipeline[R] = SessionTransport(key).embed(payload, responsePipeline)
}

/**
 * A target that discards payload stored in a session from the given response.
 *
 * @param key              The session key in which the payload will be transported.
 * @param responsePipeline The response pipeline.
 * @tparam R The type of the response.
 */
final case class DiscardFromSession[R](key: String)(
  implicit
  responsePipeline: ResponsePipeline[R]
) extends Target[ResponsePipeline[R]] {

  /**
   * Discards payload from a session.
   *
   * @return The response pipeline.
   */
  override def write: ResponsePipeline[R] = SessionTransport(key).discard(responsePipeline)
}
