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

import java.time.{ Clock, Instant }

import cats.effect.Sync
import io.circe.Json
import silhouette.RichInstant._
import silhouette.authenticator.Validator._
import silhouette.http.{ Request, RequestPipeline }
import silhouette.{ Credentials, LoginInfo }

import scala.concurrent.duration._

/**
 * An authenticator tracks an authenticated user.
 *
 * The authenticator can use sliding window expiration. This means that the authenticator times
 * out after a certain time if it wasn't used. This can be controlled with the [[touched]]
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
 * @param touched     Maybe the instant of time the authenticator was last touched.
 * @param expires     Maybe the instant of time the authenticator expires.
 * @param fingerprint Maybe a fingerprint of the user.
 * @param tags        A list of tags to tag the authenticator.
 * @param payload     Some custom payload an authenticator can transport.
 */
final case class Authenticator(
  id: String,
  loginInfo: LoginInfo,
  touched: Option[Instant] = None,
  expires: Option[Instant] = None,
  fingerprint: Option[String] = None,
  tags: Seq[String] = Seq(),
  payload: Option[Json] = None
) extends Credentials {

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
  def touchedAt(clock: Clock): Option[FiniteDuration] = touched.map(clock.instant() - _)

  /**
   * Touches an authenticator.
   *
   * This enables sliding windows functionality for an authenticator.
   *
   * @param clock The clock instance.
   * @return A touched authenticator.
   */
  def touch(clock: Clock): Authenticator = copy(touched = Some(clock.instant()))

  /**
   * Returns a copy of this authenticator with an expire instant of time.
   *
   * @param expiry The duration an authenticator expires in.
   * @param clock  The clock implementation to get the current time.
   * @return A copy of this authenticator with an expire instant of time.
   */
  def withExpiry(expiry: FiniteDuration, clock: Clock): Authenticator =
    copy(expires = Some(clock.instant() + expiry))

  /**
   * Returns a copy of this authenticator with a default fingerprint.
   *
   * @param request The request.
   * @return A copy of this authenticator with a default fingerprint.
   */
  def withFingerPrint()(implicit request: Request): Authenticator =
    copy(fingerprint = Some(request.fingerprint()))

  /**
   * Returns a copy of this authenticator with a custom fingerprint.
   *
   * @param requestPipeline      The request pipeline.
   * @param fingerprintGenerator The fingerprint generator.
   * @return A copy of this authenticator with a custom fingerprint.
   */
  def withFingerPrint[R](fingerprintGenerator: RequestPipeline[R] => String)(
    implicit
    requestPipeline: RequestPipeline[R]
  ): Authenticator =
    copy(fingerprint = Some(requestPipeline.fingerprint(fingerprintGenerator)))

  /**
   * Returns a copy of this authenticator with new tags added.
   *
   * @param tags The new tags to add.
   * @return A copy of this authenticator with new tags added.
   */
  def withTags(tags: String*): Authenticator = copy(tags = this.tags ++ tags)

  /**
   * Indicates if the authenticator was touched.
   *
   * @return True if the authenticator was touched, false otherwise.
   */
  def isTouched: Boolean = touched.isDefined

  /**
   * Indicates if this authenticator is tagged with all the given tags.
   *
   * @param tags The tags to check for.
   * @return True if this authenticator is tagged with all the given tags, false otherwise.
   */
  def isTaggedWith(tags: String*): Boolean = tags.forall(this.tags.contains)

  /**
   * Checks if the authenticator is valid.
   *
   * @param validators The list of validators to validate the authenticator with.
   * @tparam F The type of the IO monad.
   * @return True if the authenticator is valid, false otherwise.
   */
  def isValid[F[_]: Sync](validators: Set[Validator[F]]): F[Status] = {
    import cats.instances.list._
    import cats.syntax.foldable._
    import cats.syntax.traverse._
    Sync[F].map(validators.map(_.isValid(this)).toList.sequence)(_.sequence_)
  }
}
