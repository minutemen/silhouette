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
import silhouette.{ Identity, LoginInfo }

import scala.concurrent.{ ExecutionContext, Future }
import scala.language.postfixOps

/**
 * An authentication pipeline which reads a serialized representation of an authenticator from a source and
 * transforms it to an authentication state.
 *
 * @param reads          The reads which transforms a string into an authenticator.
 * @param identityReader The reader to retrieve the [[Identity]] for the [[LoginInfo]] stored in the
 *                       [[silhouette.authenticator.Authenticator]] from the persistence layer.
 * @param validators     The list of validators to apply to the [[silhouette.authenticator.Authenticator]].
 * @param authorization  The [[Authorization]] to apply to the [[silhouette.authenticator.Authenticator]] and
 *                       the [[Identity]].
 */
final case class ReadsAuthenticationPipeline[I <: Identity](
  reads: Reads,
  identityReader: LoginInfo => Future[Option[I]],
  override val validators: Set[Validator] = Set(),
  override val authorization: Authorization[I] = Authorized()
)(
  implicit
  ec: ExecutionContext
) extends AuthenticationPipeline[Option[String], I] {

  /**
   * Apply the pipeline.
   *
   * @param source The source to read the authenticator from.
   * @return An authentication state.
   */
  override def read(source: Option[String]): Future[State[I]] = source andThenFuture reads toState
}
