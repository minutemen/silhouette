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

import silhouette.Credentials
import silhouette.http._
import silhouette.http.transport.format.{ BasicAuthHeaderFormat, BearerAuthHeaderFormat }
import silhouette.util.Fitting._

import scala.language.postfixOps

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
 * A reads that tries to retrieve some payload, stored in a header, from the given request.
 *
 * @param name The name of the header in which the payload will be transported.
 * @tparam R The type of the request.
 */
final case class RetrieveFromHeader[R](name: String) extends RetrieveReads[R, String] {

  /**
   * Reads payload from a request header.
   *
   * @param requestPipeline The request pipeline.
   * @return The retrieved payload.
   */
  override def read(requestPipeline: RequestPipeline[R]): Option[String] =
    HeaderTransport(name).retrieve(requestPipeline)
}

/**
 * A reads that tries to retrieve a bearer token, stored in a header, from the given request.
 *
 * Reads from header in the form "Authorization: Bearer some.token".
 *
 * @param name The name of the header in which the payload will be transported; Defaults to Authorization.
 * @tparam R The type of the request.
 */
final case class RetrieveBearerTokenFromHeader[R](name: String = "Authorization")
  extends RetrieveReads[R, String] {

  /**
   * Reads payload from a header.
   *
   * @param requestPipeline The request pipeline.
   * @return The retrieved payload.
   */
  override def read(requestPipeline: RequestPipeline[R]): Option[String] =
    RetrieveFromHeader[R](name)(requestPipeline) andThenTry BearerAuthHeaderFormat() toOption
}

/**
 * A reads that tries to retrieve basic credentials, stored in a header, from the given request.
 *
 * Reads from header in the form "Authorization: Basic user:password".
 *
 * @param name The name of the header in which the payload will be transported; Defaults to Authorization.
 * @tparam R The type of the request.
 */
final case class RetrieveBasicCredentialsFromHeader[R](name: String = "Authorization")
  extends RetrieveReads[R, Credentials] {

  /**
   * Reads payload from a header.
   *
   * @param requestPipeline The request pipeline.
   * @return The retrieved payload.
   */
  override def read(requestPipeline: RequestPipeline[R]): Option[Credentials] =
    RetrieveFromHeader[R](name)(requestPipeline) andThenTry BasicAuthHeaderFormat() toOption
}

/**
 * A writes that smuggles a header with the given payload into the given request.
 *
 * @param name The name of the header in which the payload will be transported.
 * @tparam R The type of the response.
 */
final case class SmuggleIntoHeader[R](name: String) extends SmuggleWrites[R, String] {

  /**
   * Merges some payload and a [[RequestPipeline]] into a [[RequestPipeline]] that contains a header with the
   * given payload as value.
   *
   * @param in A tuple consisting of the payload to smuggle in a header and the [[RequestPipeline]] in which the header
   *           should be smuggled.
   * @return The request pipeline with the smuggled header.
   */
  override def write(in: (String, RequestPipeline[R])): RequestPipeline[R] =
    HeaderTransport(name).smuggle[R] _ tupled in
}

/**
 * A writes that smuggles a header with a bearer token into the given request.
 *
 * Smuggles a header in the form "Authorization: Bearer some.token".
 *
 * @param name The name of the header in which the payload will be transported; Defaults to Authorization.
 * @tparam R The type of the response.
 */
final case class SmuggleBearerTokenIntoHeader[R](name: String = "Authorization")
  extends SmuggleWrites[R, String] {

  /**
   * Merges some token and a [[RequestPipeline]] into a [[RequestPipeline]] that contains a bearer token header with
   * the given token as value.
   *
   * @param in A tuple consisting of the token to smuggle in a bearer token header and the [[RequestPipeline]] in
   *           which the header should be smuggled.
   * @return The request pipeline with the smuggled header.
   */
  override def write(in: (String, RequestPipeline[R])): RequestPipeline[R] =
    BearerAuthHeaderFormat().mapW(value => value -> in._2) andThen SmuggleIntoHeader[R](name) write in._1
}

/**
 * A writes that smuggles a header with basic credentials into the given request.
 *
 * Smuggles a header in the form "Authorization: Basic user:password".
 *
 * @param name The name of the header in which the payload will be transported; Defaults to Authorization.
 * @tparam R The type of the response.
 */
final case class SmuggleBasicCredentialsIntoHeader[R](name: String = "Authorization")
  extends SmuggleWrites[R, Credentials] {

  /**
   * Merges some credentials and a [[RequestPipeline]] into a [[RequestPipeline]] that contains a basic auth header
   * with the given credentials as value.
   *
   * @param in A tuple consisting of the credentials to smuggle in a basic auth header and the [[RequestPipeline]] in
   *           which the header should be smuggled.
   * @return The request pipeline with the smuggled header.
   */
  override def write(in: (Credentials, RequestPipeline[R])): RequestPipeline[R] =
    BasicAuthHeaderFormat().mapW(value => value -> in._2) andThen SmuggleIntoHeader[R](name) write in._1
}

/**
 * A writes that embeds a header with the given payload into the given response.
 *
 * @param name The name of the header in which the payload will be transported.
 * @tparam R The type of the response.
 */
final case class EmbedIntoHeader[R](name: String) extends EmbedWrites[R, String] {

  /**
   * Merges some payload and a [[ResponsePipeline]] into a [[ResponsePipeline]] that contains a header with the
   * given payload as value.
   *
   * @param in A tuple consisting of the payload to embed in a header and the [[ResponsePipeline]] in which the header
   *           should be embedded.
   * @return The response pipeline with the embedded header.
   */
  override def write(in: (String, ResponsePipeline[R])): ResponsePipeline[R] =
    HeaderTransport(name).embed[R] _ tupled in
}

/**
 * A writes that embeds a header with the given bearer token into the given response.
 *
 * Embeds a header in the form "Authorization: Bearer some.token".
 *
 * @param name The name of the header in which the payload will be transported; Defaults to Authorization.
 * @tparam R The type of the response.
 */
final case class EmbedBearerTokenIntoHeader[R](name: String = "Authorization") extends EmbedWrites[R, String] {

  /**
   * Merges some token and a [[ResponsePipeline]] into a [[ResponsePipeline]] that contains a bearer token header with
   * the given token as value.
   *
   * @param in A tuple consisting of the token to embed in a bearer token header and the [[ResponsePipeline]] in
   *           which the header should be embedded.
   * @return The response pipeline with the embedded header.
   */
  override def write(in: (String, ResponsePipeline[R])): ResponsePipeline[R] =
    BearerAuthHeaderFormat().mapW(value => value -> in._2) andThen EmbedIntoHeader[R](name) write in._1
}

/**
 * A writes that embeds a header with the given basic credentials into the given response.
 *
 * Embeds a header in the form "Authorization: Basic user:password".
 *
 * @param name The name of the header in which the payload will be transported; Defaults to Authorization.
 * @tparam R The type of the response.
 */
final case class EmbedBasicCredentialsIntoHeader[R](name: String = "Authorization")
  extends EmbedWrites[R, Credentials] {

  /**
   * Merges some credentials and a [[ResponsePipeline]] into a [[ResponsePipeline]] that contains a basic auth header
   * with the given credentials as value.
   *
   * @param in A tuple consisting of the credentials to embed in a basic auth header and the [[ResponsePipeline]] in
   *           which the header should be embedded.
   * @return The response pipeline with the embedded header.
   */
  override def write(in: (Credentials, ResponsePipeline[R])): ResponsePipeline[R] =
    BasicAuthHeaderFormat().mapW(value => value -> in._2) andThen EmbedIntoHeader[R](name) write in._1
}
