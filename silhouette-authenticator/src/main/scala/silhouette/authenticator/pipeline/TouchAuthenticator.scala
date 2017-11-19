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
package silhouette.authenticator.pipeline

import java.time.Clock

import silhouette.Authenticator
import silhouette.authenticator.TransformPipeline

import scala.concurrent.Future

/**
 * Pipeline which touches an authenticator.
 *
 * The authenticator can use sliding window expiration. This means that the authenticator times
 * out after a certain time if it wasn't used. This pipeline touches an authenticator to indicate
 * that it was used. It will only touch an authenticator if it was previously touched, because sliding
 * window expiration is an opt-in feature which isn't enabled by default. So you can always mixin
 * this pipeline and it will only touch the authenticator if touching is explicitly enabled by the
 * authenticator on its creation.
 *
 * @param clock The clock instance.
 */
final case class TouchAuthenticator(clock: Clock) extends TransformPipeline {

  /**
   * Apply the pipeline.
   *
   * @param authenticator The authenticator.
   * @return The response pipeline that discards the authenticator client side.
   */
  override def apply(authenticator: Authenticator): Future[Authenticator] = Future.successful {
    if (authenticator.isTouched) {
      authenticator.touch(clock)
    } else {
      authenticator
    }
  }
}
