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

import java.time.{ Clock, Instant }

import silhouette.Authenticator.Implicits._
import silhouette.authenticator.Validator
import silhouette.http.RequestPipeline

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }
import scala.json.ast.JObject

/**
 * An authenticator tracks an authenticated user.
 *
 * @param id          The ID of the authenticator.
 * @param loginInfo   The linked login info for an identity.
 * @param lastUsed    The instant of time the authenticator was last used.
 * @param expires     The instant of time the authenticator expires.
 * @param tags        A list of tags to tag the authenticator.
 * @param fingerprint Maybe a fingerprint of the user.
 * @param payload     Some custom payload an authenticator can transport.
 */
case class Authenticator(
  id: String,
  loginInfo: LoginInfo,
  lastUsed: Instant,
  expires: Instant,
  tags: Seq[String] = Seq(),
  fingerprint: Option[String] = None,
  payload: Option[JObject] = None) {

  /**
   * A marker flag which indicates that an authenticator value was changed.
   */
  protected val touched = false

  /**
   * Copies the authenticator and touches it.
   *
   * @param id          The ID of the authenticator.
   * @param loginInfo   The linked login info for an identity.
   * @param lastUsed    The instant of time the authenticator was last used.
   * @param expires     The instant of time the authenticator expires.
   * @param tags        A list of tags to tag the authenticator.
   * @param fingerprint Maybe a fingerprint of the user.
   * @param payload     Some custom payload an authenticator can transport.
   * @return The new touched authenticator.
   */
  def touch(
    id: String = id,
    loginInfo: LoginInfo = loginInfo,
    lastUsed: Instant = lastUsed,
    expires: Instant = expires,
    tags: Seq[String] = tags,
    fingerprint: Option[String] = fingerprint,
    payload: Option[JObject] = payload
  ): Authenticator = new Authenticator(
    id,
    loginInfo,
    lastUsed,
    expires,
    tags,
    fingerprint,
    payload
  ) {
    override protected val touched = true
  }

  /**
   * Gets the duration the authenticator expires in.
   *
   * @param clock The clock instance.
   * @return The duration the authenticator expires in.
   */
  def expiresIn(clock: Clock): FiniteDuration = expires - clock.instant()

  /**
   * Gets the duration the authenticator was last used at.
   *
   * @param clock The clock instance.
   * @return The duration the authenticator was last used at.
   */
  def lastUsedAt(clock: Clock): FiniteDuration = clock.instant() - lastUsed

  /**
   * Returns a copy of this authenticator with new tags added.
   *
   * @param tag The new tag to add.
   * @return A copy of this authenticator with new tags added.
   */
  def withTag(tag: String): Authenticator = copy(tags = tags :+ tag)

  /**
   * Returns a copy of this authenticator with a default fingerprint.
   *
   * @param requestPipeline The request pipeline.
   * @return A copy of this authenticator with a default fingerprint.
   */
  def withFingerPrint[R]()(implicit requestPipeline: RequestPipeline[R]): Authenticator =
    copy(fingerprint = Some(requestPipeline.fingerprint))

  /**
   * Returns a copy of this authenticator with a custom fingerprint.
   *
   * @param requestPipeline      The request pipeline.
   * @param fingerprintGenerator The fingerprint generator.
   * @return A copy of this authenticator with a custom fingerprint.
   */
  def withFingerPrint[R](fingerprintGenerator: R => String)(
    implicit
    requestPipeline: RequestPipeline[R]
  ): Authenticator =
    copy(fingerprint = Some(requestPipeline.fingerprint(fingerprintGenerator)))

  /**
   * Indicates if this authenticator is tagged with the given tag.
   *
   * @param tag The tag to check for.
   * @return True if this authenticator is tagged with he given tag, false otherwise.
   */
  def isTaggedWith(tag: String): Boolean = tags.contains(tag)

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
   * Creates a new authenticator.
   *
   * This method sets the `lastUsed` property based on the current instant and the `expires` property based on the
   * current instant plus the expiry duration.
   *
   * @param id        The ID of the authenticator.
   * @param loginInfo The linked login info for an identity.
   * @param expiry    The authentication expiry.
   * @param clock     The clock implementation to get the current time.
   * @return An authenticator instance.
   */
  def apply(id: String, loginInfo: LoginInfo, expiry: FiniteDuration, clock: Clock): Authenticator = {
    val now = clock.instant()
    Authenticator(
      id,
      loginInfo,
      lastUsed = now,
      expires = now + expiry
    )
  }

  /**
   * Some implicits.
   */
  object Implicits {

    /**
     * Defines additional methods on an `DateTime` instance.
     *
     * @param instant The [[java.time.Instant]] instance on which the additional methods should be defined.
     */
    implicit class RichInstant(instant: Instant) {

      /**
       * Adds a duration to an instant.
       *
       * @param duration The duration to add.
       * @return An [[Instant]] instance with the added duration.
       */
      def +(duration: FiniteDuration): Instant = {
        instant.plusMillis(duration.toMillis)
      }

      /**
       * Subtracts an instant from an instant and returns the duration of it.
       *
       * @param sub The instant to subtract.
       * @return An [[Instant]] instance with the added instance.
       */
      def -(sub: Instant): FiniteDuration = {
        FiniteDuration(instant.minusMillis(sub.toEpochMilli).toEpochMilli, MILLISECONDS)
      }

      /**
       * Subtracts a duration from an instant.
       *
       * @param duration The duration to subtract.
       * @return A [[Instant]] instance with the subtracted duration.
       */
      def -(duration: FiniteDuration): Instant = {
        instant.minusMillis(duration.toMillis)
      }
    }
  }
}
