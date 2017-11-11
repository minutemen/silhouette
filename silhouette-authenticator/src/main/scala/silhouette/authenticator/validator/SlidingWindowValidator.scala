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
package silhouette.authenticator.validator

import java.time.Clock

import silhouette.Authenticator
import silhouette.authenticator.Validator

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ ExecutionContext, Future }

/**
 * A validator that checks if an [[Authenticator]] has timed out after a certain time if it hasn't been used.
 *
 * An [[Authenticator]] can use a sliding window expiration. This means that the [[Authenticator]] times out
 * after a certain time if it hasn't been used. So it checks if the time elapsed since the last time the
 * [[Authenticator]] was used, is longer than the maximum idle timeout specified as [[idleTimeout]] argument
 * of the validator.
 *
 * If the [[Authenticator.lastTouched]] property isn't set, then this validator returns always true.
 *
 * @param idleTimeout The duration an [[Authenticator]] can be idle before it timed out.
 * @param clock       The clock implementation to validate against.
 */
final case class SlidingWindowValidator(idleTimeout: FiniteDuration, clock: Clock) extends Validator {

  /**
   * Checks if the [[Authenticator]] is valid.
   *
   * @param authenticator The [[Authenticator]] to validate.
   * @param ec            The execution context to perform the async operations.
   * @return True if the [[Authenticator]] is valid, false otherwise.
   */
  override def isValid(authenticator: Authenticator)(
    implicit
    ec: ExecutionContext
  ): Future[Boolean] = Future.successful {
    authenticator.lastTouched.forall(_.plusSeconds(idleTimeout.toSeconds).isBefore(clock.instant()))
  }
}
