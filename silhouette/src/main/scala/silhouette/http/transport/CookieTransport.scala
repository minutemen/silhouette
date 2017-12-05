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

import scala.concurrent.duration.FiniteDuration

/**
 * The settings for the cookie transport.
 *
 * @param name     The cookie name.
 * @param path     The cookie path.
 * @param domain   The cookie domain.
 * @param secure   Whether this cookie is secured, sent only for HTTPS requests.
 * @param httpOnly Whether this cookie is HTTP only, i.e. not accessible from client-side JavaScript code.
 * @param maxAge   The duration a cookie expires. `None` for a transient cookie.
 */
final case class CookieTransportSettings(
  name: String,
  path: String = "/",
  domain: Option[String] = None,
  secure: Boolean = true,
  httpOnly: Boolean = true,
  maxAge: Option[FiniteDuration] = None
) extends TransportSettings

/**
 * The cookie transport.
 *
 * @param settings The transport settings.
 */
final case class CookieTransport(settings: CookieTransportSettings)
  extends RetrieveFromRequest
  with SmuggleIntoRequest
  with EmbedIntoResponse
  with DiscardFromResponse {

  /**
   * Retrieves the payload, stored in a cookie, from request.
   *
   * @param request The request pipeline to retrieve the payload from.
   * @tparam R The type of the request.
   * @return Some payload or None if no payload could be found in request.
   */
  override def retrieve[R](request: RequestPipeline[R]): Option[String] =
    request.cookie(settings.name).map(_.value)

  /**
   * Adds a cookie with the given payload to the request.
   *
   * @param payload The payload to smuggle into the request.
   * @param request The request pipeline.
   * @tparam R The type of the request.
   * @return The manipulated request pipeline.
   */
  override def smuggle[R](payload: String, request: RequestPipeline[R]): RequestPipeline[R] =
    request.withCookies(cookie(payload))

  /**
   * Adds a cookie with the given payload to the response.
   *
   * @param payload  The payload to embed into the response.
   * @param response The response pipeline to manipulate.
   * @tparam R The type of the response.
   * @return The manipulated response pipeline.
   */
  override def embed[R](payload: String, response: ResponsePipeline[R]): ResponsePipeline[R] =
    response.withCookies(cookie(payload))

  /**
   * Discards the cookie on the client.
   *
   * @param response The response pipeline to manipulate.
   * @tparam R The type of the response.
   * @return The manipulated response pipeline.
   */
  override def discard[R](response: ResponsePipeline[R]): ResponsePipeline[R] = {
    val discardingCookie = cookie(value = "").copy(maxAge = Some(-86400))
    response.withCookies(discardingCookie)
  }

  /**
   * Creates a cookie based on the settings and the given value.
   *
   * @param value The cookie value.
   * @return A cookie value.
   */
  private def cookie(value: String) = Cookie(
    name = settings.name,
    value = value,
    maxAge = settings.maxAge.map(_.toSeconds.toInt),
    domain = settings.domain,
    path = Some(settings.path),
    secure = settings.secure,
    httpOnly = settings.httpOnly
  )
}

/**
 * A reads that tries to retrieve some payload, stored in a cookie, from the given request.
 *
 * @param name The name of the cookie to retrieve to payload from.
 * @tparam R The type of the request.
 */
final case class RetrieveFromCookie[R](name: String) extends RetrieveReads[R, String] {

  /**
   * Reads payload from a cookie stored in the given request.
   *
   * @param requestPipeline The request pipeline.
   * @return The retrieved payload.
   */
  override def read(requestPipeline: RequestPipeline[R]): Option[String] =
    CookieTransport(CookieTransportSettings(name)).retrieve(requestPipeline)
}

/**
 * A writes that smuggles a cookie with the given payload into the given request.
 *
 * @param settings The cookie transport settings.
 * @tparam R The type of the request.
 */
final case class SmuggleIntoCookie[R](settings: CookieTransportSettings) extends SmuggleWrites[R, String] {

  /**
   * Merges some payload and a [[RequestPipeline]] into a [[RequestPipeline]] that contains a cookie with the
   * given payload as value.
   *
   * @param in A tuple consisting of the payload to embed in a cookie and the [[RequestPipeline]] in which the cookie
   *           should be embedded.
   * @return The request pipeline with the smuggled cookie.
   */
  override def write(in: (String, RequestPipeline[R])): RequestPipeline[R] =
    CookieTransport(settings).smuggle[R] _ tupled in
}

/**
 * A writes that embeds a cookie with the given payload into the given response.
 *
 * @param settings The cookie transport settings.
 * @tparam R The type of the response.
 */
final case class EmbedIntoCookie[R](settings: CookieTransportSettings) extends EmbedWrites[R, String] {

  /**
   * Merges some payload and a [[ResponsePipeline]] into a [[ResponsePipeline]] that contains a cookie with the
   * given payload as value.
   *
   * @param in A tuple consisting of the payload to embed in a cookie and the [[ResponsePipeline]] in which the cookie
   *           should be embedded.
   * @return The response pipeline with the embedded cookie.
   */
  override def write(in: (String, ResponsePipeline[R])): ResponsePipeline[R] =
    CookieTransport(settings).embed[R] _ tupled in
}

/**
 * A writes that embeds a discarding cookie into the given response.
 *
 * @param settings The transport settings.
 * @tparam R The type of the response.
 */
final case class DiscardFromCookie[R](settings: CookieTransportSettings) extends DiscardWrites[R] {

  /**
   * Takes a [[ResponsePipeline]] and embeds a discarding cookie into it.
   *
   * @param responsePipeline The response pipeline in which the discarding cookie should be embedded.
   * @return The response pipeline with the embedded discarding cookie.
   */
  override def write(responsePipeline: ResponsePipeline[R]): ResponsePipeline[R] =
    CookieTransport(settings).discard(responsePipeline)
}
