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

import java.time.Instant

import silhouette.http._
import sttp.model.CookieWithMeta

import scala.concurrent.duration.FiniteDuration

/**
 * The config for the cookie transport.
 *
 * @param name            The cookie name.
 * @param path            The cookie path.
 * @param domain          The cookie domain.
 * @param secure          Whether this cookie is secured, sent only for HTTPS requests.
 * @param httpOnly        Whether this cookie is HTTP only, i.e. not accessible from client-side JavaScript code.
 * @param expires         The maximum lifetime of the cookie. `None` for a transient cookie.
 * @param maxAge          The duration a cookie expires. `None` for a transient cookie.
 * @param otherDirectives Like the SameSite directive.
 */
final case class CookieTransportConfig(
  name: String,
  path: String = "/",
  domain: Option[String] = None,
  secure: Boolean = true,
  httpOnly: Boolean = true,
  expires: Option[Instant] = None,
  maxAge: Option[FiniteDuration] = None,
  otherDirectives: Map[String, Option[String]] = Map()
) extends TransportConfig

/**
 * The cookie transport.
 *
 * @param config The transport config.
 */
final case class CookieTransport(config: CookieTransportConfig)
  extends RetrieveFromRequest
  with SmuggleIntoRequest
  with EmbedIntoResponse
  with DiscardFromResponse {

  /**
   * Retrieves the payload, stored in a cookie, from request.
   *
   * @param request The request pipeline to retrieve the payload from.
   * @return Some payload or None if no payload could be found in request.
   */
  override def retrieve(request: Request): Option[String] = request.cookie(config.name).map(_.value)

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
  override def discard[R](response: ResponsePipeline[R]): ResponsePipeline[R] =
    response.withCookies(cookie(value = "").maxAge(Some(-86400)))

  /**
   * Creates a cookie based on the config and the given value.
   *
   * @param value The cookie value.
   * @return A cookie value.
   */
  private def cookie(value: String) = CookieWithMeta.unsafeApply(
    name = config.name,
    value = value,
    expires = config.expires,
    maxAge = config.maxAge.map(_.toSeconds.toInt),
    domain = config.domain,
    path = Some(config.path),
    secure = config.secure,
    httpOnly = config.httpOnly,
    otherDirectives = config.otherDirectives
  )
}

/**
 * A function that tries to retrieve some payload, stored in a cookie, from the given request.
 *
 * @param name The name of the cookie to retrieve to payload from.
 */
final case class RetrieveFromCookie(name: String) extends Retrieve[String] {

  /**
   * Reads payload from a cookie stored in the given request.
   *
   * @param request The request.
   * @return The retrieved payload.
   */
  override def apply(request: Request): Option[String] =
    CookieTransport(CookieTransportConfig(name)).retrieve(request)
}

/**
 * A function that smuggles a cookie with the given payload into the given request.
 *
 * @param config The cookie transport config.
 * @tparam R The type of the request.
 */
final case class SmuggleIntoCookie[R](config: CookieTransportConfig) extends Smuggle[String, R] {

  /**
   * Merges some payload and a [[RequestPipeline]] into a [[RequestPipeline]] that contains a cookie with the
   * given payload as value.
   *
   * @param requestPipeline The [[RequestPipeline]] in which the cookie should be embedded.
   * @return A function that gets the payload and which returns the request pipeline with the smuggled cookie.
   */
  override def apply(requestPipeline: RequestPipeline[R]): String => RequestPipeline[R] =
    (payload: String) => CookieTransport(config).smuggle(payload, requestPipeline)
}

/**
 * A function that embeds a cookie with the given payload into the given response.
 *
 * @param config The cookie transport config.
 * @tparam R The type of the response.
 */
final case class EmbedIntoCookie[R](config: CookieTransportConfig) extends Embed[String, R] {

  /**
   * Merges some payload and a [[ResponsePipeline]] into a [[ResponsePipeline]] that contains a cookie with the
   * given payload as value.
   *
   * @param responsePipeline The [[ResponsePipeline]] in which the cookie should be embedded.
   * @return A function that gets the payload and which returns the response pipeline with the embedded cookie.
   */
  override def apply(responsePipeline: ResponsePipeline[R]): String => ResponsePipeline[R] =
    (payload: String) => CookieTransport(config).embed(payload, responsePipeline)
}

/**
 * A function that discards a cookie on the client.
 *
 * @param config The transport config.
 * @tparam R The type of the response.
 */
final case class DiscardCookie[R](config: CookieTransportConfig) extends Discard[R] {

  /**
   * Embeds a discard cookie into the [[ResponsePipeline]].
   *
   * @param responsePipeline The response pipeline in which the discard cookie should be embedded.
   * @return The response pipeline with the embedded discard cookie.
   */
  override def apply(responsePipeline: ResponsePipeline[R]): ResponsePipeline[R] =
    CookieTransport(config).discard(responsePipeline)
}
