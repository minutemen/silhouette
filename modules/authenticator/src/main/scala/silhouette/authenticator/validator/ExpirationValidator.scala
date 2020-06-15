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

import cats.data.Validated._
import cats.effect.Async
import silhouette.authenticator.Validator._
import silhouette.authenticator.validator.ExpirationValidator._
import silhouette.authenticator.{ Authenticator, Validator }

import scala.concurrent.duration._

/**
 * A validator that checks if an [[Authenticator]] is expired.
 *
 * If the [[Authenticator.expires]] property isn't set, then this validator returns always true.
 *
 * @param clock The clock implementation to validate against.
 * @tparam F The type of the IO monad.
 */
final case class ExpirationValidator[F[_]: Async](clock: Clock) extends Validator[F] {

  /**
   * Checks if the [[Authenticator]] is valid.
   *
   * @param authenticator The [[Authenticator]] to validate.
   * @return [[cats.data.Validated]] if the authenticator is valid or invalid.
   */
  override def isValid(authenticator: Authenticator): F[Status] =
    Async[F].pure {
      if (authenticator.expiresIn(clock).forall(_ >= 0.millis))
        validNel(())
      else
        invalidNel(Error.format(authenticator.expiresIn(clock).map(_.neg()).getOrElse(0.millis)))
    }
}

/**
 * The companion object.
 */
object ExpirationValidator {
  val Error = "Authenticator is expired %s ago"
}
