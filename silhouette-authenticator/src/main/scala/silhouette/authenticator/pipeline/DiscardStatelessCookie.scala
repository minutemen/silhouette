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
package silhouette.authenticator.pipeline

import silhouette.Authenticator
import silhouette.authenticator.WritePipeline
import silhouette.http.ResponsePipeline
import silhouette.http.transport.{ CookieTransportSettings, DiscardFromCookie }

/**
 * Discards a stateless cookie.
 *
 * @param responsePipeline The response pipeline from which the authenticator should be discarded.
 * @param cookieSettings   The cookie transport settings.
 * @tparam R The type of the response.
 */
case class DiscardStatelessCookie[R](responsePipeline: ResponsePipeline[R], cookieSettings: CookieTransportSettings)
  extends WritePipeline[ResponsePipeline[R]] {

  /**
   * Apply the pipeline.
   *
   * @param authenticator The authenticator.
   * @return The response pipeline that discards the authenticator client side.
   */
  override def apply(authenticator: Authenticator): ResponsePipeline[R] =
    DiscardFromCookie(cookieSettings)(responsePipeline).write
}
