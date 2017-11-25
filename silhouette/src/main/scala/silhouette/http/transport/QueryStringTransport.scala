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
 * The query string transport.
 */
final case class QueryStringTransport(name: String)
  extends RetrieveFromRequest
  with SmuggleIntoRequest {

  /**
   * Retrieves the payload, stored in the query string, from request.
   *
   * @param request The request pipeline to retrieve the payload from.
   * @tparam R The type of the request.
   * @return Some value or None if no payload could be found in request.
   */
  override def retrieve[R](request: RequestPipeline[R]): Option[String] =
    request.queryParam(name).headOption

  /**
   * Adds a query param with the given payload to the request.
   *
   * @param payload The payload to smuggle into the request.
   * @param request The request pipeline.
   * @tparam R The type of the request.
   * @return The manipulated request pipeline.
   */
  override def smuggle[R](payload: String, request: RequestPipeline[R]): RequestPipeline[R] =
    request.withQueryParams(name -> payload)
}

/**
 * A reads that tries to retrieve some payload, stored in a query param, from the given request.
 *
 * @param name The name of the query param in which the payload will be transported.
 * @tparam R The type of the request.
 */
final case class RetrieveFromQueryString[R](name: String) extends RetrieveReads[R, String] {

  /**
   * Reads payload from a query string param stored in the given request.
   *
   * @param requestPipeline The request pipeline.
   * @return The retrieved payload.
   */
  override def read(requestPipeline: RequestPipeline[R]): Option[String] =
    QueryStringTransport(name).retrieve(requestPipeline)
}

/**
 * A writes that smuggles a query param with the given payload into the given request.
 *
 * @param name The name of the query param in which the payload will be transported.
 * @tparam R The type of the response.
 */
final case class SmuggleIntoQueryString[R](name: String) extends SmuggleWrites[R, String] {

  /**
   * Merges some payload and a [[RequestPipeline]] into a [[RequestPipeline]] that contains a query param with the
   * given payload as value.
   *
   * @param in A tuple consisting of the payload to embed in a query param and the [[RequestPipeline]] in which the
   *           query param should be embedded.
   * @return The request pipeline.
   */
  override def write(in: (String, RequestPipeline[R])): RequestPipeline[R] =
    QueryStringTransport(name).smuggle[R] _ tupled in
}
