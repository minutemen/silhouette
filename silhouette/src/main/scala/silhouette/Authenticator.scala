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

import java.time.ZonedDateTime

import silhouette.Authenticator.Implicits._

import scala.concurrent.duration.FiniteDuration

/**
 * An authenticator tracks an authenticated user.
 */
trait Authenticator {

  /**
   * The Type of the generated value an authenticator will be serialized to.
   */
  type Value

  /**
   * The type of the settings an authenticator can handle.
   */
  type Settings

  /**
   * Gets the linked login info for an identity.
   *
   * @return The linked login info for an identity.
   */
  def loginInfo: LoginInfo

  /**
   * Checks if the authenticator valid.
   *
   * @return True if the authenticator valid, false otherwise.
   */
  def isValid: Boolean
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
     * @param dateTime The `DateTime` instance on which the additional methods should be defined.
     */
    implicit class RichDateTime(dateTime: ZonedDateTime) {

      /**
       * Adds a duration to a date/time.
       *
       * @param duration The duration to add.
       * @return A date/time instance with the added duration.
       */
      def +(duration: FiniteDuration): ZonedDateTime = {
        dateTime.plusSeconds(duration.toSeconds)
      }

      /**
       * Subtracts a duration from a date/time.
       *
       * @param duration The duration to subtract.
       * @return A date/time instance with the subtracted duration.
       */
      def -(duration: FiniteDuration): ZonedDateTime = {
        dateTime.minusSeconds(duration.toSeconds)
      }
    }
  }
}

/**
 * An authenticator which can be stored in a backing store.
 */
trait StorableAuthenticator extends Authenticator {

  /**
   * Gets the ID to reference the authenticator in the backing store.
   *
   * @return The ID to reference the authenticator in the backing store.
   */
  def id: String
}

/**
 * An authenticator that may expire.
 */
trait ExpirableAuthenticator extends Authenticator {

  /**
   * The last used date/time.
   */
  val lastUsedDateTime: ZonedDateTime

  /**
   * The expiration date/time.
   */
  val expirationDateTime: ZonedDateTime

  /**
   * The duration an authenticator can be idle before it timed out.
   */
  val idleTimeout: Option[FiniteDuration]

  /**
   * Checks if the authenticator isn't expired and isn't timed out.
   *
   * @return True if the authenticator isn't expired and isn't timed out.
   */
  override def isValid: Boolean = !isExpired && !isTimedOut

  /**
   * Checks if the authenticator is expired. This is an absolute timeout since the creation of
   * the authenticator.
   *
   * @return True if the authenticator is expired, false otherwise.
   */
  def isExpired: Boolean = expirationDateTime.isBefore(ZonedDateTime.now())

  /**
   * Checks if the time elapsed since the last time the authenticator was used, is longer than
   * the maximum idle timeout specified in the properties.
   *
   * @return True if sliding window expiration is activated and the authenticator is timed out, false otherwise.
   */
  def isTimedOut: Boolean = idleTimeout.isDefined && (lastUsedDateTime + idleTimeout.get).isBefore(ZonedDateTime.now())
}
