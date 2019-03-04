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
package silhouette.provider

import silhouette._
import silhouette.http.RequestPipeline

import scala.concurrent.Future

/**
 * A provider which can be hooked into a request.
 *
 * It scans the request for credentials and returns the [[AuthState]] for it.
 *
 * @tparam R The type of the request.
 * @tparam I The type of the identity.
 */
trait RequestProvider[R, I <: Identity] extends Provider {

  /**
   * The type of the credentials.
   */
  type C <: Credentials

  /**
   * Authenticates an identity based on credentials sent in a request.
   *
   * @param request The request pipeline.
   * @return The [[AuthState]].
   */
  def authenticate(request: RequestPipeline[R]): Future[AuthState[I, C]]
}
