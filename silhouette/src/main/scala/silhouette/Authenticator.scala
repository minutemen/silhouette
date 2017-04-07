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

import java.time.Instant

import silhouette.authenticator.AuthenticatorValidator
import silhouette.http.RequestPipeline

import scala.concurrent.{ ExecutionContext, Future }
import scala.json.ast.JObject

/**
 * An authenticator tracks an authenticated user.
 *
 * @param id                 The ID of the authenticator.
 * @param loginInfo          The linked login info for an identity.
 * @param lastUsedDateTime   The last used date/time.
 * @param expirationDateTime The expiration date/time.
 * @param fingerprint        Maybe a fingerprint of the user.
 * @param payload            Some custom payload an authenticator can transport.
 */
final case class Authenticator(
  id: String,
  loginInfo: LoginInfo,
  lastUsedDateTime: Instant,
  expirationDateTime: Instant,
  fingerprint: Option[String] = None,
  payload: Option[JObject] = None) {

  /**
   * Checks if the authenticator is valid.
   *
   * @param validators The list of validators to validate the authenticator with.
   * @param request    The request pipeline.
   * @param ec         The execution context to perform the async operations.
   * @return True if the authenticator is valid, false otherwise.
   */
  def isValid[R](validators: Set[AuthenticatorValidator])(
    implicit
    request: RequestPipeline[R],
    ec: ExecutionContext
  ): Future[Boolean] = {
    Future.sequence(validators.map(_.isValid(this))).map(!_.exists(!_))
  }
}
