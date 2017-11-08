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

import silhouette.authenticator._
import silhouette.authenticator.format.JwtReads
import silhouette.util.Fitting._
import silhouette.util.Source
import silhouette.{ Identity, LoginInfo, jwt }

import scala.concurrent.{ ExecutionContext, Future }
import scala.language.postfixOps

/**
 * An authentication pipeline which reads a serialized JWT from a source and transforms it to an authentication state.
 *
 * @param jwtReads       The JWT reads implementation used to unserialize the JWT.
 * @param identityReader The reader to retrieve the [[Identity]] for the [[LoginInfo]] stored in the
 *                       [[silhouette.Authenticator]] from the persistence layer.
 * @param validators     The list of validators to apply to the [[silhouette.Authenticator]].
 * @param authorization  The [[Authorization]] to apply to the [[Identity]].
 */
final case class JwtAuthentication[I <: Identity](
  jwtReads: jwt.Reads,
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
  override def apply(source: Source[Option[String]]): Future[State[I]] = {
    source.read andThenFuture JwtReads(jwtReads) toState
  }
}
