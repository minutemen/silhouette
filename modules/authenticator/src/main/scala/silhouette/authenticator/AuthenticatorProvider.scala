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

import cats.effect.Sync
import com.typesafe.scalalogging.LazyLogging
import javax.inject.Inject
import silhouette._
import silhouette.authenticator.AuthenticatorProvider._
import silhouette.http.{ RequestPipeline, ResponsePipeline }
import silhouette.provider.RequestProvider

/**
 * A request provider implementation that supports authentication with an [[Authenticator]].
 *
 * @param authenticationPipeline The authentication pipeline which transforms a request into an [[AuthState]].
 * @param targetPipeline         The target pipeline which writes an [[Authenticator]] to the [[ResponsePipeline]].
 * @tparam F The type of the IO monad.
 * @tparam R The type of the request.
 * @tparam P The type of the response.
 * @tparam I The type of the identity.
 */
class AuthenticatorProvider[F[_]: Sync, R, P, I <: Identity] @Inject() (
  authenticationPipeline: AuthenticationPipeline[F, RequestPipeline[R], I],
  targetPipeline: TargetPipeline[F, ResponsePipeline[P]]
) extends RequestProvider[F, R, P, I] with LazyLogging {

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
   * @param handler A function that returns a [[ResponsePipeline]] for the given [[AuthState]].
   * @return The [[ResponsePipeline]].
   */
  override def authenticate(request: RequestPipeline[R])(handler: AuthStateHandler): F[ResponsePipeline[P]] = {
    Sync[F].flatMap(authenticationPipeline(request)) {
      case authState @ Authenticated(_, authenticator, _) =>
        Sync[F].flatMap(handler(authState))(targetPipeline(authenticator, _))
      case authState =>
        handler(authState)
    }
  }
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
