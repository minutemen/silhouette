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
 * The authenticator can use sliding window expiration. This means that the authenticator times
 * out after a certain time if it wasn't used. This can be controlled with the [[lastTouched]]
 * property and the [[silhouette.authenticator.validator.SlidingWindowValidator]].
 *
 * The expiry of the authenticator can be controlled with the [[expires]] property and the
 * [[silhouette.authenticator.validator.ExpirationValidator]].
 *
 * The authenticator can store an identity-related fingerprint that can be used to build an additional
 * security mechanism, by validating the fingerprint on subsequent requests. This can be controlled with
 * the [[fingerprint]] property and the [[silhouette.authenticator.validator.FingerprintValidator]].
 *
 * @param id          The ID of the authenticator.
 * @param loginInfo   The linked login info for an identity.
 * @param lastTouched Maybe the instant of time the authenticator was last touched.
 * @param expires     Maybe the instant of time the authenticator expires.
 * @param fingerprint Maybe a fingerprint of the user.
 * @param tags        A list of tags to tag the authenticator.
 * @param payload     Some custom payload an authenticator can transport.
 */
final case class Authenticator(
  id: String,
  loginInfo: LoginInfo,
  lastTouched: Option[Instant] = None,
  expires: Option[Instant] = None,
  fingerprint: Option[String] = None,
  tags: Seq[String] = Seq(),
  payload: Option[JObject] = None) {

  /**
   * Gets the duration the authenticator expires in.
   *
   * @param clock The clock instance.
   * @return If it returns None then the authenticator doesn't expire, otherwise it returns the duration the
   *         authenticator expires in.
   */
  def expiresIn(clock: Clock): Option[FiniteDuration] = expires.map(_ - clock.instant())

  /**
   * Gets the duration the authenticator was last touched at.
   *
   * @param clock The clock instance.
   * @return If it returns None then it wasn't touched, otherwise it returns the duration the authenticator
   *         was last touched at.
   */
  def lastTouchedAt(clock: Clock): Option[FiniteDuration] = lastTouched.map(clock.instant() - _)

  /**
   * Touches an authenticator.
   *
   * This enables sliding windows functionality for an authenticator.
   *
   * @param clock The clock instance.
   * @return A touched authenticator.
   */
  def touch(clock: Clock): Authenticator = copy(lastTouched = Some(clock.instant()))

  /**
   * Returns a copy of this authenticator with an expire instant of time.
   *
   * @param expiry    The authentication expiry.
   * @param clock     The clock implementation to get the current time.
   * @return A copy of this authenticator with an expire instant of time.
   */
  def withExpiry(expiry: FiniteDuration, clock: Clock): Authenticator =
    copy(expires = Some(clock.instant() + expiry))

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
   * Returns a copy of this authenticator with new tags added.
   *
   * @param tag The new tag to add.
   * @return A copy of this authenticator with new tags added.
   */
  def withTag(tag: String): Authenticator = copy(tags = tags :+ tag)

  /**
   * Indicates if the authenticator was touched.
   *
   * @return True if the authenticator was touched, false otherwise.
   */
  def isTouched: Boolean = lastTouched.isDefined

  /**
   * Indicates if this authenticator is tagged with the given tag.
   *
   * @param tag The tag to check for.
   * @return True if this authenticator is tagged with he given tag, false otherwise.
   */
  def isTaggedWith(tag: String): Boolean = tags.contains(tag)

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
     * @param instant The [[java.time.Instant]] instance on which the additional methods should be defined.
     */
    implicit class RichInstant(instant: Instant) {

      /**
       * Adds a duration to an instant.
       *
       * @param duration The duration to add.
       * @return An [[java.time.Instant]] instance with the added duration.
       */
      def +(duration: FiniteDuration): Instant = {
        instant.plusMillis(duration.toMillis)
      }

      /**
       * Subtracts an instant from an instant and returns the duration of it.
       *
       * @param sub The instant to subtract.
       * @return An [[java.time.Instant]] instance with the added instance.
       */
      def -(sub: Instant): FiniteDuration = {
        FiniteDuration(instant.minusMillis(sub.toEpochMilli).toEpochMilli, MILLISECONDS)
      }

      /**
       * Subtracts a duration from an instant.
       *
       * @param duration The duration to subtract.
       * @return A [[java.time.Instant]] instance with the subtracted duration.
       */
      def -(duration: FiniteDuration): Instant = {
        instant.minusMillis(duration.toMillis)
      }
    }
  }
}
