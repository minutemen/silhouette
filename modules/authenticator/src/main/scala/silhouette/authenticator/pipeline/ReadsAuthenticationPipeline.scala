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

import silhouette.Fitting._
import silhouette.authenticator._
import silhouette.{ AuthState, Identity, LoginInfo }

import scala.concurrent.{ ExecutionContext, Future }

/**
 * An authentication pipeline which reads an authenticator from a source and transforms it to an authentication state.
 *
 * @param reads          The reads which transforms a source into an authenticator.
 * @param identityReader The reader to retrieve the [[Identity]] for the [[LoginInfo]] stored in the
 *                       [[silhouette.authenticator.Authenticator]] from the persistence layer.
 * @param validators     The list of validators to apply to the [[silhouette.authenticator.Authenticator]].
 * @tparam S The type of the source.
 * @tparam I The type of the identity.
 */
final case class ReadsAuthenticationPipeline[S, I <: Identity](
  reads: Reads[S],
  override protected val identityReader: LoginInfo => Future[Option[I]],
  override protected val validators: Set[Validator] = Set()
)(
  implicit
  ec: ExecutionContext
) extends AuthenticationPipeline[Option[S], I] {

  /**
   * Apply the pipeline.
   *
   * @param source The source to read the authenticator from.
   * @return An authentication state.
   */
  override def read(source: Option[S]): Future[AuthState[I, Authenticator]] = source.andThenFuture(reads).toState
}
