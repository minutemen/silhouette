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
package silhouette.provider.social

import silhouette.AuthInfo
import silhouette.http.{ RequestPipeline, ResponsePipeline, SilhouetteResponse }
import silhouette.provider.social.state.{ State, StateHandler }

/**
 * A stateful auth info, wraps the `AuthInfo` with state.
 *
 * @param authInfo The auth info.
 * @param state    The state params returned from the provider.
 * @tparam A The type of the auth info.
 */
case class StatefulAuthInfo[A <: AuthInfo](authInfo: A, state: State)

/**
 * Extends the [[SocialProvider]] with the ability to handle provider specific state.
 *
 * @tparam F The type of the IO monad.
 * @tparam C The type of the config.
 */
trait SocialStateProvider[F[_], C] extends SocialProvider[F, C] {

  /**
   * Authenticates the user and returns the auth information and the state params passed to the provider.
   *
   * Returns either a [[StatefulAuthInfo]] if all went OK or a `ResponsePipeline` that the controller
   * sends to the browser (e.g.: in the case of OAuth where the user needs to be redirected to the service
   * provider).
   *
   * @param request      The request pipeline.
   * @param stateHandler The state handler instance which handles the state serialization/deserialization.
   * @tparam R The type of the request.
   * @return Either the [[silhouette.http.ResponsePipeline]] on the left or the [[StatefulAuthInfo]] from the
   *         provider on the right.
   */
  def authenticate[R](
    request: RequestPipeline[R],
    stateHandler: StateHandler[F]
  ): F[Either[ResponsePipeline[SilhouetteResponse], StatefulAuthInfo[A]]]
}
