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

import silhouette.Authenticator
import silhouette.authenticator.Validator

import scala.concurrent.{ ExecutionContext, Future }

/**
 * A validator that checks if an [[Authenticator]] is located in a backing store.
 *
 * If an authenticator for the given [[Authenticator]] was found in the store, then the validator returns true,
 * otherwise it returns false. The validator can be used as a blacklist or whitelist validator.
 *
 * @param validator A validator to validate the [[Authenticator]] against a backing store.
 */
final case class BackingStoreValidator(validator: Authenticator => Future[Boolean]) extends Validator {

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
  ): Future[Boolean] = validator(authenticator)
}
