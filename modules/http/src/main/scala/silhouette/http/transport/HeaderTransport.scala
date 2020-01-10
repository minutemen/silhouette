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
import silhouette.http.auth.{ BasicAuthSchemeReader, BasicAuthSchemeWriter, BearerAuthSchemeReader, BearerAuthSchemeWriter }

/**
 * The header transport.
 *
 * @param name The name of the header in which the payload will be transported.
 */
final case class HeaderTransport(name: Header.Name)
  extends RetrieveFromRequest
  with SmuggleIntoRequest
  with EmbedIntoResponse {

  /**
   * Retrieves the payload, stored in a header, from request.
   *
   * @param request The request pipeline to retrieve the payload from.
   * @return Some payload or None if no payload could be found in request.
   */
  override def retrieve(request: Request): Option[String] = request.header(name).map(_.value)

  /**
   * Adds a header with the given payload to the request.
   *
   * @param payload The payload to smuggle into the request.
   * @param request The request pipeline.
   * @tparam R The type of the request.
   * @return The manipulated request pipeline.
   */
  override def smuggle[R](payload: String, request: RequestPipeline[R]): RequestPipeline[R] =
    request.withHeaders(Header(name, payload))

  /**
   * Adds a header with the given payload to the response.
   *
   * @param payload  The payload to embed into the response.
   * @param response The response pipeline to manipulate.
   * @tparam R The type of the response.
   * @return The manipulated response pipeline.
   */
  override def embed[R](payload: String, response: ResponsePipeline[R]): ResponsePipeline[R] =
    response.withHeaders(Header(name, payload))
}

/**
 * A function that tries to retrieve some payload, stored in a header, from the given request.
 *
 * @param name The name of the header in which the payload will be transported.
 */
final case class RetrieveFromHeader(name: Header.Name) extends Retrieve[String] {

  /**
   * Reads payload from a request header.
   *
   * @param request The request.
   * @return The retrieved payload.
   */
  override def apply(request: Request): Option[String] = HeaderTransport(name).retrieve(request)
}

/**
 * A function that tries to retrieve a bearer token, stored in a header, from the given request.
 *
 * Reads from header in the form "Authorization: Bearer some.token".
 *
 * @param name The name of the header in which the payload will be transported; Defaults to Authorization.
 */
final case class RetrieveBearerTokenFromHeader(name: Header.Name = Header.Name.Authorization)
  extends Retrieve[BearerToken] {

  /**
   * Reads a bearer token from a header.
   *
   * @param request The request.
   * @return The retrieved payload.
   */
  override def apply(request: Request): Option[BearerToken] =
    RetrieveFromHeader(name)(request).flatMap(payload => BearerAuthSchemeReader(payload).toOption)
}

/**
 * A function that tries to retrieve basic credentials, stored in a header, from the given request.
 *
 * Reads from header in the form "Authorization: Basic user:password".
 *
 * @param name The name of the header in which the payload will be transported; Defaults to Authorization.
 */
final case class RetrieveBasicCredentialsFromHeader(name: Header.Name = Header.Name.Authorization)
  extends Retrieve[BasicCredentials] {

  /**
   * Reads payload from a header.
   *
   * @param request The request.
   * @return The retrieved payload.
   */
  override def apply(request: Request): Option[BasicCredentials] =
    RetrieveFromHeader(name)(request).flatMap(payload => BasicAuthSchemeReader(payload).toOption)
}

/**
 * A function that smuggles a header with the given payload into the given request.
 *
 * @param name            The name of the header in which the payload will be transported.
 * @param requestPipeline The [[RequestPipeline]] in which the header should be smuggled.
 * @tparam R The type of the request.
 */
final case class SmuggleIntoHeader[R](name: Header.Name)(
  protected val requestPipeline: RequestPipeline[R]
) extends Smuggle[String, R] {

  /**
   * Merges some payload and a [[RequestPipeline]] into a [[RequestPipeline]] that contains a header with the
   * given payload as value.
   *
   * @param payload The payload to smuggle in a header.
   * @return The request pipeline with the smuggled header.
   */
  override def apply(payload: String): RequestPipeline[R] =
    HeaderTransport(name).smuggle(payload, requestPipeline)
}

/**
 * A function that smuggles a header with a bearer token into the given request.
 *
 * Smuggles a header in the form "Authorization: Bearer some.token".
 *
 * @param name            The name of the header in which the payload will be transported; Defaults to Authorization.
 * @param requestPipeline The [[RequestPipeline]] in which the header should be smuggled.
 * @tparam R The type of the request.
 */
final case class SmuggleBearerTokenIntoHeader[R](name: Header.Name = Header.Name.Authorization)(
  protected val requestPipeline: RequestPipeline[R]
) extends Smuggle[BearerToken, R] {

  /**
   * Merges some token and a [[RequestPipeline]] into a [[RequestPipeline]] that contains a bearer token header with
   * the given token as value.
   *
   * @param bearerToken The token to smuggle in a bearer token header.
   * @return The request pipeline with the smuggled header.
   */
  override def apply(bearerToken: BearerToken): RequestPipeline[R] =
    SmuggleIntoHeader[R](name)(requestPipeline)(BearerAuthSchemeWriter(bearerToken))
}

/**
 * A function that smuggles a header with basic credentials into the given request.
 *
 * Smuggles a header in the form "Authorization: Basic user:password".
 *
 * @param name            The name of the header in which the payload will be transported; Defaults to Authorization.
 * @param requestPipeline The [[RequestPipeline]] in which the header should be smuggled.
 * @tparam R The type of the request.
 */
final case class SmuggleBasicCredentialsIntoHeader[R](name: Header.Name = Header.Name.Authorization)(
  protected val requestPipeline: RequestPipeline[R]
) extends Smuggle[BasicCredentials, R] {

  /**
   * Merges some credentials and a [[RequestPipeline]] into a [[RequestPipeline]] that contains a basic auth header
   * with the given credentials as value.
   *
   * @param credentials     The credentials to smuggle in a basic auth header.
   * @return The request pipeline with the smuggled header.
   */
  override def apply(credentials: BasicCredentials): RequestPipeline[R] =
    SmuggleIntoHeader[R](name)(requestPipeline)(BasicAuthSchemeWriter(credentials))
}

/**
 * A function that embeds a header with the given payload into the given response.
 *
 * @param name             The name of the header in which the payload will be transported.
 * @param responsePipeline The [[ResponsePipeline]] in which the header should be embedded.
 * @tparam R The type of the response.
 */
final case class EmbedIntoHeader[R](name: Header.Name)(
  protected val responsePipeline: ResponsePipeline[R]
) extends Embed[String, R] {

  /**
   * Merges some payload and a [[ResponsePipeline]] into a [[ResponsePipeline]] that contains a header with the
   * given payload as value.
   *
   * @param payload The payload to embed in a header.
   * @return The response pipeline with the embedded header.
   */
  override def apply(payload: String): ResponsePipeline[R] =
    HeaderTransport(name).embed(payload, responsePipeline)
}

/**
 * A function that embeds a header with the given bearer token into the given response.
 *
 * Embeds a header in the form "Authorization: Bearer some.token".
 *
 * @param name             The name of the header in which the payload will be transported; Defaults to Authorization.
 * @param responsePipeline The [[ResponsePipeline]] in which the header should be embedded.
 * @tparam R The type of the response.
 */
final case class EmbedBearerTokenIntoHeader[R](name: Header.Name = Header.Name.Authorization)(
  protected val responsePipeline: ResponsePipeline[R]
) extends Embed[BearerToken, R] {

  /**
   * Merges some token and a [[ResponsePipeline]] into a [[ResponsePipeline]] that contains a bearer token header with
   * the given token as value.
   *
   * @param bearerToken The token to embed in a bearer token header.
   * @return The response pipeline with the embedded header.
   */
  override def apply(bearerToken: BearerToken): ResponsePipeline[R] =
    EmbedIntoHeader(name)(responsePipeline)(BearerAuthSchemeWriter(bearerToken))
}

/**
 * A function that embeds a header with the given basic credentials into the given response.
 *
 * Embeds a header in the form "Authorization: Basic user:password".
 *
 * @param name             The name of the header in which the payload will be transported; Defaults to Authorization.
 * @param responsePipeline The [[ResponsePipeline]] in which the header should be embedded.
 * @tparam R The type of the response.
 */
final case class EmbedBasicCredentialsIntoHeader[R](name: Header.Name = Header.Name.Authorization)(
  protected val responsePipeline: ResponsePipeline[R]
) extends Embed[BasicCredentials, R] {

  /**
   * Merges some credentials and a [[ResponsePipeline]] into a [[ResponsePipeline]] that contains a basic auth header
   * with the given credentials as value.
   *
   * @param credentials The credentials to embed in a basic auth header.
   * @return The response pipeline with the embedded header.
   */
  override def apply(credentials: BasicCredentials): ResponsePipeline[R] =
    EmbedIntoHeader(name)(responsePipeline)(BasicAuthSchemeWriter(credentials))
}
