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

import cats.data.Validated._
import cats.effect.Sync
import silhouette.authenticator.Validator._
import silhouette.authenticator.validator.FingerprintValidator._
import silhouette.authenticator.{ Authenticator, Validator }

/**
 * A validator that checks if the stored fingerprint is the same as the current fingerprint.
 *
 * If the [[Authenticator]] has no fingerprint stored, then this validator returns always true.
 *
 * @param fingerprint The fingerprint to check against.
 * @tparam F The type of the IO monad.
 */
final case class FingerprintValidator[F[_]: Sync](fingerprint: String) extends Validator[F] {

  /**
   * Checks if the [[Authenticator]] is valid.
   *
   * @param authenticator The [[Authenticator]] to validate.
   * @return [[cats.data.Validated.Valid]] if the authenticator is valid, [[cats.data.Validated.Invalid]] otherwise.
   */
  override def isValid(authenticator: Authenticator): F[Status] = Sync[F].pure {
    if (authenticator.fingerprint.forall(_ == fingerprint)) {
      validNel(())
    } else {
      invalidNel(Error.format(fingerprint, authenticator.fingerprint.getOrElse("")))
    }
  }
}

/**
 * The companion object.
 */
object FingerprintValidator {
  val Error = "Fingerprint `%s` doesn't match the authenticators fingerprint `%s`"
}
