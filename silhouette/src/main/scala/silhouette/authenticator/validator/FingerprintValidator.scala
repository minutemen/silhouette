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

import silhouette.http.RequestPipeline
import silhouette.util.FingerprintGenerator
import silhouette.Authenticator
import silhouette.authenticator.AuthenticatorValidator

import scala.concurrent.{ ExecutionContext, Future }

/**
 * A validator that checks if the stored fingerprint is the same as the current fingerprint.
 *
 * If the authenticator has no fingerprint stored, then this validator returns always true.
 *
 * @param fingerprintGenerator The fingerprint generator.
 */
final case class FingerprintValidator(fingerprintGenerator: FingerprintGenerator)
  extends AuthenticatorValidator {

  /**
   * Checks if the authenticator is valid.
   *
   * @param authenticator The authenticator to validate.
   * @param request       The request pipeline.
   * @param ec            The execution context to perform the async operations.
   * @tparam R The type of the request.
   * @return True if the authenticator is valid, false otherwise.
   */
  override def isValid[R](authenticator: Authenticator)(
    implicit
    request: RequestPipeline[R],
    ec: ExecutionContext
  ): Future[Boolean] = Future.successful {
    authenticator.fingerprint match {
      case None              => true
      case Some(fingerprint) => fingerprintGenerator.generate == fingerprint
    }
  }
}