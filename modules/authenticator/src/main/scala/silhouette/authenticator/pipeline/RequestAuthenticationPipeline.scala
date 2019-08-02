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

import silhouette.authenticator.{ AuthenticationPipeline, Authenticator, Validator }
import silhouette.http.RequestPipeline
import silhouette.{ AuthState, Identity, LoginInfo }

import scala.concurrent.{ ExecutionContext, Future }

/**
 * An authentication pipeline which reads an authenticator from an HTTP request and transforms it to an
 * authentication state.
 *
 * @param handler        The handler which transforms an HTTP request into an authenticator.
 * @param identityReader The reader to retrieve the [[Identity]] for the [[LoginInfo]] stored in the
 *                       [[silhouette.authenticator.Authenticator]] from the persistence layer.
 * @param validators     The list of validators to apply to the [[silhouette.authenticator.Authenticator]].
 * @param ec             The execution context.
 * @tparam R The type of the request.
 * @tparam I The type of the identity.
 */
final case class RequestAuthenticationPipeline[R, I <: Identity](
  handler: RequestPipeline[R] => Future[Option[Authenticator]],
  override protected val identityReader: LoginInfo => Future[Option[I]],
  override protected val validators: Set[Validator] = Set()
)(
  implicit
  ec: ExecutionContext
) extends AuthenticationPipeline[RequestPipeline[R], I] {

  /**
   * Apply the pipeline.
   *
   * @param requestPipeline The request pipeline to retrieve the authenticator from.
   * @return An authentication state.
   */
  override def read(requestPipeline: RequestPipeline[R]): Future[AuthState[I, Authenticator]] = {
    handler(requestPipeline).toState
  }
}
