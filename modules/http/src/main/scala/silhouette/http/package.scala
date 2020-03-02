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
package silhouette

import java.net.URI

import sttp.model.Header

/**
 * HTTP related interfaces and implementations.
 */
package object http {

  /**
   * A function that gets a response and returns a response. Can be used as type for functions that writes to
   * a response.
   */
  type ResponseWriter[R] = ResponsePipeline[R] => ResponsePipeline[R]

  /**
   * The fake request and response definitions. Useful for testing.
   */
  protected[silhouette] object Fake {
    type Request = SilhouetteRequest
    type Response = SilhouetteResponse
    type RequestPipeline = http.RequestPipeline[Request]
    type ResponsePipeline = http.ResponsePipeline[Response]

    def request(
      uri: URI = new URI("http://localhost/"),
      method: Method = Method.GET,
      headers: Seq[Header] = Seq(),
      cookies: Seq[Cookie] = Seq(),
      queryParams: Map[String, Seq[String]] = Map()
    ): RequestPipeline = SilhouetteRequestPipeline(SilhouetteRequest(
      uri,
      method,
      headers,
      cookies,
      queryParams
    ))
    def request: RequestPipeline = request()

    val response: ResponsePipeline = SilhouetteResponsePipeline(SilhouetteResponse(Status.OK))
  }
}
