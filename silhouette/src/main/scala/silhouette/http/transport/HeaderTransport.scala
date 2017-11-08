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

import com.typesafe.scalalogging.LazyLogging
import silhouette.Credentials
import silhouette.http._
import silhouette.http.transport.format.{ BasicAuthHeaderFormat, BearerAuthHeaderFormat }
import silhouette.util.Fitting._
import silhouette.util.{ Source, Target }

/**
 * The header transport.
 *
 * @param name The name of the header in which the payload will be transported.
 */
final case class HeaderTransport(name: String)
  extends RetrieveFromRequest
  with SmuggleIntoRequest
  with EmbedIntoResponse {

  /**
   * Retrieves the payload, stored in a header, from request.
   *
   * @param request The request pipeline to retrieve the payload from.
   * @tparam R The type of the request.
   * @return Some payload or None if no payload could be found in request.
   */
  override def retrieve[R](request: RequestPipeline[R]): Option[String] =
    request.header(name).headOption

  /**
   * Adds a header with the given payload to the request.
   *
   * @param payload The payload to smuggle into the request.
   * @param request The request pipeline.
   * @tparam R The type of the request.
   * @return The manipulated request pipeline.
   */
  override def smuggle[R](payload: String, request: RequestPipeline[R]): RequestPipeline[R] =
    request.withHeaders(name -> payload)

  /**
   * Adds a header with the given payload to the response.
   *
   * @param payload  The payload to embed into the response.
   * @param response The response pipeline to manipulate.
   * @tparam R The type of the response.
   * @return The manipulated response pipeline.
   */
  override def embed[R](payload: String, response: ResponsePipeline[R]): ResponsePipeline[R] =
    response.withHeaders(name -> payload)
}

/**
 * A source that tries to retrieve some payload, stored in a header, from the given request.
 *
 * @param name             The name of the header in which the payload will be transported.
 * @param requestPipeline  The request pipeline.
 * @tparam R The type of the request.
 */
case class RetrieveFromHeader[R](name: String)(
  implicit
  requestPipeline: RequestPipeline[R]
) extends Source[Option[String]] {

  /**
   * Retrieves payload from a header.
   *
   * @return The retrieved payload.
   */
  override def read: Option[String] = HeaderTransport(name).retrieve(requestPipeline)
}

/**
 * A source that tries to retrieve a bearer token, stored in a header, from the given request.
 *
 * @param name             The name of the header in which the payload will be transported; Defaults to Authorization.
 * @param requestPipeline  The request pipeline.
 * @tparam R The type of the request.
 */
case class RetrieveBearerTokenFromHeader[R](name: String = "Authorization")(
  implicit
  requestPipeline: RequestPipeline[R]
) extends Source[Option[String]] with LazyLogging {

  /**
   * Retrieves payload from a header.
   *
   * @return The retrieved payload.
   */
  override def read: Option[String] = {
    RetrieveFromHeader(name)
      .read
      .andThenTry(BearerAuthHeaderFormat().asReads)
      .toTry
      .fold(e => { logger.info(e.getMessage, e); None }, v => Some(v))
  }
}

/**
 * A source that tries to retrieve basic credentials, stored in a header, from the given request.
 *
 * @param name             The name of the header in which the payload will be transported; Defaults to Authorization.
 * @param requestPipeline  The request pipeline.
 * @tparam R The type of the request.
 */
case class RetrieveBasicCredentialsFromHeader[R](name: String = "Authorization")(
  implicit
  requestPipeline: RequestPipeline[R]
) extends Source[Option[Credentials]] with LazyLogging {

  /**
   * Retrieves payload from a header.
   *
   * @return The retrieved payload.
   */
  override def read: Option[Credentials] = {
    RetrieveFromHeader(name)
      .read
      .andThenTry(BasicAuthHeaderFormat().asReads)
      .toTry
      .fold(e => { logger.info(e.getMessage, e); None }, v => Some(v))
  }
}

/**
 * A target that smuggles a header with the given payload into the given request.
 *
 * @param payload          The payload to embed.
 * @param name             The name of the header in which the payload will be transported.
 * @param requestPipeline  The request pipeline.
 * @tparam R The type of the response.
 */
final case class SmuggleIntoHeader[R](payload: String, name: String)(
  implicit
  requestPipeline: RequestPipeline[R]
) extends Target[RequestPipeline[R]] {

  /**
   * Smuggles payload into a header.
   *
   * @return The request pipeline.
   */
  override def write: RequestPipeline[R] = HeaderTransport(name).smuggle(payload, requestPipeline)
}

/**
 * A target that smuggles a header with the a bearer token into the given request.
 *
 * @param token            The bearer token to embed.
 * @param name             The name of the header in which the payload will be transported; Defaults to Authorization.
 * @param requestPipeline  The request pipeline.
 * @tparam R The type of the response.
 */
final case class SmuggleBearerTokenIntoHeader[R](token: String, name: String = "Authorization")(
  implicit
  requestPipeline: RequestPipeline[R]
) extends Target[RequestPipeline[R]] {

  /**
   * Smuggles a bearer token into a header.
   *
   * @return The request pipeline.
   */
  override def write: RequestPipeline[R] = SmuggleIntoHeader(BearerAuthHeaderFormat().write(token), name).write
}

/**
 * A target that smuggles a header with the basic credentials into the given request.
 *
 * @param credentials      The credentials to embed.
 * @param name             The name of the header in which the payload will be transported; Defaults to Authorization.
 * @param requestPipeline  The request pipeline.
 * @tparam R The type of the response.
 */
final case class SmuggleBasicCredentialsIntoHeader[R](credentials: Credentials, name: String = "Authorization")(
  implicit
  requestPipeline: RequestPipeline[R]
) extends Target[RequestPipeline[R]] {

  /**
   * Smuggles basic credentials into a header.
   *
   * @return The request pipeline.
   */
  override def write: RequestPipeline[R] = SmuggleIntoHeader(BasicAuthHeaderFormat().write(credentials), name).write
}

/**
 * A target that embeds a header with the given payload into the given response.
 *
 * @param payload          The payload to embed.
 * @param name             The name of the header in which the payload will be transported.
 * @param responsePipeline The response pipeline.
 * @tparam R The type of the response.
 */
case class EmbedIntoHeader[R](payload: String, name: String)(
  implicit
  responsePipeline: ResponsePipeline[R]
) extends Target[ResponsePipeline[R]] {

  /**
   * Embeds payload into a header.
   *
   * @return The response pipeline.
   */
  override def write: ResponsePipeline[R] = HeaderTransport(name).embed(payload, responsePipeline)
}

/**
 * A target that embeds a header with the given bearer token into the given response.
 *
 * @param token            The bearer token to embed.
 * @param name             The name of the header in which the payload will be transported; Defaults to Authorization.
 * @param responsePipeline The response pipeline.
 * @tparam R The type of the response.
 */
case class EmbedBearerTokenIntoHeader[R](token: String, name: String)(
  implicit
  responsePipeline: ResponsePipeline[R]
) extends Target[ResponsePipeline[R]] {

  /**
   * Embeds a bearer token into a header.
   *
   * @return The response pipeline.
   */
  override def write: ResponsePipeline[R] = EmbedIntoHeader(BearerAuthHeaderFormat().write(token), name).write
}

/**
 * A target that embeds a header with the given basic credentials into the given response.
 *
 * @param credentials      The credentials to embed.
 * @param name             The name of the header in which the payload will be transported; Defaults to Authorization.
 * @param responsePipeline The response pipeline.
 * @tparam R The type of the response.
 */
case class EmbedBasicCredentialsIntoHeader[R](credentials: Credentials, name: String)(
  implicit
  responsePipeline: ResponsePipeline[R]
) extends Target[ResponsePipeline[R]] {

  /**
   * Embeds a bearer token into a header.
   *
   * @return The response pipeline.
   */
  override def write: ResponsePipeline[R] = EmbedIntoHeader(BasicAuthHeaderFormat().write(credentials), name).write
}
