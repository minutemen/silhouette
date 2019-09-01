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
package silhouette.authenticator

import com.typesafe.scalalogging.LazyLogging
import javax.inject.Inject
import silhouette._
import silhouette.authenticator.AuthenticatorProvider._
import silhouette.http.RequestPipeline
import silhouette.provider.RequestProvider

import scala.concurrent.Future

/**
 * A request provider implementation that supports authentication with an [[Authenticator]].
 *
 * @param pipeline The authentication pipeline which transforms a request into an [[AuthState]].
 * @tparam R The type of the request.
 * @tparam I The type of the identity.
 */
class AuthenticatorProvider[R, I <: Identity] @Inject() (
  pipeline: AuthenticationPipeline[RequestPipeline[R], I]
) extends RequestProvider[R, I] with LazyLogging {

  /**
   * The type of the credentials.
   */
  override type C = Authenticator

  /**
   * Gets the provider ID.
   *
   * @return The provider ID.
   */
  override def id: String = ID

  /**
   * Authenticates an identity based on an [[Authenticator]] sent in a request.
   *
   * @param request The request pipeline.
   * @return The [[AuthState]].
   */
  override def authenticate(request: RequestPipeline[R]): Future[AuthState[I, Authenticator]] =
    pipeline.read(request)
}

/**
 * The companion object.
 */
object AuthenticatorProvider {

  /**
   * The provider constants.
   */
  val ID = "authenticator"
}
