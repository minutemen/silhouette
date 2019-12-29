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

import silhouette.authenticator.Validator._

/**
 * Adds the ability to validate an [[Authenticator]].
 *
 * @tparam F The type of the IO monad.
 */
trait Validator[F[_]] {

  /**
   * Checks if the authenticator is valid.
   *
   * @param authenticator The authenticator to validate.
   * @return [[silhouette.authenticator.Validator.Valid]] if the authenticator is valid,
   *        [[silhouette.authenticator.Validator.Invalid]] otherwise.
   */
  def isValid(authenticator: Authenticator): F[Status]
}

/**
 * The companion object.
 */
object Validator {

  /**
   * The validation status.
   */
  sealed trait Status

  /**
   * Indicates that the validation was successful.
   */
  case object Valid extends Status

  /**
   * Indicates that the validation failed.
   *
   * @param errors The validation errors.
   */
  final case class Invalid(errors: Seq[String]) extends Status
}
