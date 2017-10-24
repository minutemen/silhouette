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
   * The type of the concrete implementation of this abstract type.
   */
  override type Self = CookieTransport

  /**
   * The settings type.
   */
  override type Settings = CookieTransportSettings

  /**
   * Gets a transport initialized with a new settings object.
   *
   * @param f A function which gets the settings passed and returns different settings.
   * @return An instance of the transport initialized with new settings.
   */
  override def withSettings(f: (Settings) => Settings): Self = new Self(f(settings))

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
 * A source that tries to retrieve some payload, stored in a cookie, from the given request.
 *
 * @param name             The name of the cookie to retrieve to payload from.
 * @param requestPipeline  The request pipeline.
 * @tparam R The type of the request.
 */
final case class RetrieveFromCookie[R](name: String)(
  implicit
  requestPipeline: RequestPipeline[R]
) extends Source[Option[String]] {

  /**
   * Retrieves payload from a cookie.
   *
   * @return The retrieved payload.
   */
  override def read: Option[String] = CookieTransport(CookieTransportSettings(name)).retrieve(requestPipeline)
}

/**
 * A target that smuggles a cookie with the given payload into the given request.
 *
 * @param payload          The payload to embed.
 * @param settings         The transport settings.
 * @param requestPipeline  The request pipeline.
 * @tparam R The type of the response.
 */
final case class SmuggleIntoCookie[R](payload: String, settings: CookieTransportSettings)(
  implicit
  requestPipeline: RequestPipeline[R]
) extends Target[RequestPipeline[R]] {

  /**
   * Smuggles payload into a cookie.
   *
   * @return The request pipeline.
   */
  override def write: RequestPipeline[R] = CookieTransport(settings).smuggle(payload, requestPipeline)
}

/**
 * A target that embeds a cookie with the given payload into the given response.
 *
 * @param payload          The payload to embed.
 * @param settings         The transport settings.
 * @param responsePipeline The response pipeline.
 * @tparam R The type of the response.
 */
final case class EmbedIntoCookie[R](payload: String, settings: CookieTransportSettings)(
  implicit
  responsePipeline: ResponsePipeline[R]
) extends Target[ResponsePipeline[R]] {

  /**
   * Embeds payload into a cookie.
   *
   * @return The response pipeline.
   */
  override def write: ResponsePipeline[R] = CookieTransport(settings).embed(payload, responsePipeline)
}

/**
 * A target that discards payload stored in a cookie from the given response.
 *
 * @param settings         The transport settings.
 * @param responsePipeline The response pipeline.
 * @tparam R The type of the response.
 */
final case class DiscardFromCookie[R](settings: CookieTransportSettings)(
  implicit
  responsePipeline: ResponsePipeline[R]
) extends Target[ResponsePipeline[R]] {

  /**
   * Discards payload from a cookie.
   *
   * @return The response pipeline.
   */
  override def write: ResponsePipeline[R] = CookieTransport(settings).discard(responsePipeline)
}
