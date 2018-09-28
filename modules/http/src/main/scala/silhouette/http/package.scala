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

/**
 * HTTP related interfaces and implementations.
 */
package object http {
  protected[silhouette] object Fake {
    type Request = RequestPipeline[SilhouetteRequest]
    type Response = ResponsePipeline[SilhouetteResponse]

    def request(
      uri: URI = new URI("http://localhost/"),
      method: Method = Method.GET,
      headers: Seq[Header] = Seq(),
      cookies: Seq[Cookie] = Seq(),
      session: Map[String, String] = Map(),
      queryParams: Map[String, Seq[String]] = Map()
    ): Request = SilhouetteRequestPipeline(SilhouetteRequest(
      uri,
      method,
      headers,
      cookies,
      session,
      queryParams
    ))
    def request: Request = request()

    val response: Response = SilhouetteResponsePipeline(SilhouetteResponse(Status.OK))
  }
}
