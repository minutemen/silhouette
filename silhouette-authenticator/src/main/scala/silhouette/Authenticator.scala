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

import silhouette.authenticator.Validator

import scala.concurrent.duration.FiniteDuration
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
case class Authenticator(
  id: String,
  loginInfo: LoginInfo,
  lastUsedDateTime: Instant,
  expirationDateTime: Instant,
  fingerprint: Option[String] = None,
  payload: Option[JObject] = None) {

  /**
   * A marker flag which indicates that an authenticator value was changed.
   */
  protected val touched = false

  /**
   * Copies the authenticator and touches it.
   *
   * @param id                 The ID of the authenticator.
   * @param loginInfo          The linked login info for an identity.
   * @param lastUsedDateTime   The last used date/time.
   * @param expirationDateTime The expiration date/time.
   * @param fingerprint        Maybe a fingerprint of the user.
   * @param payload            Some custom payload an authenticator can transport.
   * @return The new touched authenticator.
   */
  def copy(
    id: String,
    loginInfo: LoginInfo,
    lastUsedDateTime: Instant,
    expirationDateTime: Instant,
    fingerprint: Option[String] = None,
    payload: Option[JObject] = None
  ): Authenticator = new Authenticator(
    id,
    loginInfo,
    lastUsedDateTime,
    expirationDateTime,
    fingerprint,
    payload
  ) {
    override protected val touched = true
  }

  /**
   * Indicates if the authenticator was touched.
   *
   * @return True if the authenticator was touched, false otherwise.
   */
  def isTouched: Boolean = touched

  /**
   * Checks if the authenticator is valid.
   *
   * @param validators The list of validators to validate the authenticator with.
   * @param ec         The execution context to perform the async operations.
   * @return True if the authenticator is valid, false otherwise.
   */
  def isValid[R](validators: Set[Validator])(
    implicit
    ec: ExecutionContext
  ): Future[Boolean] = {
    Future.sequence(validators.map(_.isValid(this))).map(!_.exists(!_))
  }
}

/**
 * The `Authenticator` companion object.
 */
object Authenticator {

  /**
   * Some implicits.
   */
  object Implicits {

    /**
     * Defines additional methods on an `DateTime` instance.
     *
     * @param instant The `Instant` instance on which the additional methods should be defined.
     */
    implicit class RichInstant(instant: Instant) {

      /**
       * Adds a duration to a date/time.
       *
       * @param duration The duration to add.
       * @return An [[Instant]] instance with the added duration.
       */
      def +(duration: FiniteDuration): Instant = {
        instant.plusSeconds(duration.toSeconds)
      }

      /**
       * Subtracts a duration from a date/time.
       *
       * @param duration The duration to subtract.
       * @return A [[Instant]] instance with the subtracted duration.
       */
      def -(duration: FiniteDuration): Instant = {
        instant.minusSeconds(duration.toSeconds)
      }
    }
  }
}
