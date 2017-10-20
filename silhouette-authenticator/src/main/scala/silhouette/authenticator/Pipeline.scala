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

import java.time.Clock
import javax.inject.Inject

import silhouette.util.{ IDGenerator, Source, Target }
import silhouette.{ Authenticator, Identity, LoginInfo }
import Authenticator.Implicits._
import silhouette.http.RequestPipeline

import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration._
import scala.json.ast.JObject

/**
 * An authenticator pipeline represents a step in the authentication process which is composed of multiple single steps.
 */
sealed trait Pipeline

/**
 * Pipeline to create a new authenticator from the [[LoginInfo]]. An additional JSON object can also be passed
 * which allows to transport arbitrary data in an authenticator.
 *
 * @param pipeline The creation pipeline.
 */
final case class CreationPipeline(pipeline: (LoginInfo, Option[JObject]) => Future[Authenticator]) extends Pipeline

/**
 * Pipeline to authenticate an identity by transforming a source into an authentication state.
 *
 * An authenticator can be read from different sources. The most common case would be to read the authenticator
 * from the request. But it would also be possible to read the authenticator from an actor or any other source.
 * If the source holds a serialized representation of an authenticator, we must transform it into an authenticator
 * instance by applying an authenticator [[Reads]]. Then we could check the validity of the authenticator by applying
 * one or multiple validators. As next we retrieve an [[Identity]] for our authenticator, either from a backing store
 * or any other service. At last we could apply some authorization rules to our identity. Any of these steps in our
 * pipeline can be by represented by a state that describes the result of each step.
 *
 * @param pipeline The authentication pipeline.
 * @tparam S The type of the source.
 * @tparam I The type of the identity.
 */
final case class AuthenticationPipeline[S, I <: Identity](pipeline: Source[S] => Future[State[I]]) extends Pipeline

/**
 * Pipeline which renews the expiration of an authenticator.
 *
 * Based on the implementation, the renew method should revoke the given authenticator first, before
 * creating a new one.
 *
 * @param pipeline The renew pipeline.
 */
final case class RenewPipeline(pipeline: Authenticator => Future[Authenticator]) extends Pipeline

/**
 * Writes an authenticator to a target.
 *
 * @param pipeline The write pipeline.
 * @tparam T The type of the target.
 */
final case class WritePipeline[T](pipeline: Authenticator => Target[T]) extends Pipeline

/**
 * Some default pipelines.
 */
object Pipeline {

  /**
   * A creation pipeline.
   *
   * @param idGenerator The ID generator used to create the authenticator ID.
   * @param clock       The clock implementation.
   */
  class Creation @Inject() (idGenerator: IDGenerator, clock: Clock) {

    /**
     * The default expiry of the authenticator.
     */
    val defaultExpiry: FiniteDuration = 12.hours

    /**
     * Returns a default implementation of the [[CreationPipeline]].
     *
     * @param authenticatorExpiry The authentication expiry.
     * @param ec                  The execution context.
     * @return A default implementation of the [[CreationPipeline]].
     */
    def default(authenticatorExpiry: FiniteDuration = defaultExpiry)(
      implicit
      ec: ExecutionContext
    ): CreationPipeline = {
      create(authenticatorExpiry, None)
    }

    /**
     * Returns a default implementation of the [[CreationPipeline]] with a default fingerprint.
     *
     * @param authenticatorExpiry The authentication expiry.
     * @param requestPipeline     The request pipeline.
     * @param ec                  The execution context.
     * @return A default implementation of the [[CreationPipeline]].
     */
    def withDefaultFingerPrint[R](authenticatorExpiry: FiniteDuration = defaultExpiry)(
      implicit
      requestPipeline: RequestPipeline[R],
      ec: ExecutionContext
    ): CreationPipeline = {
      create(authenticatorExpiry, Some(requestPipeline.fingerprint))
    }

    /**
     * Returns a default implementation of the [[CreationPipeline]] with a custom fingerprint.
     *
     * @param authenticatorExpiry  The authentication expiry.
     * @param requestPipeline      The request pipeline.
     * @param fingerprintGenerator The fingerprint generator.
     * @param ec                  The execution context.
     * @return A default implementation of the [[CreationPipeline]].
     */
    def withCustomFingerPrint[R](
      authenticatorExpiry: FiniteDuration = defaultExpiry,
      fingerprintGenerator: R => String
    )(
      implicit
      requestPipeline: RequestPipeline[R],
      ec: ExecutionContext
    ): CreationPipeline = {
      create(authenticatorExpiry, Some(requestPipeline.fingerprint(fingerprintGenerator)))
    }

    /**
     * Creates an authenticator,
     *
     * @param authenticatorExpiry The duration an authenticator expires after it was created.
     * @param fingerprint         A fingerprint of the user
     * @param ec                  The execution context.
     * @return A [[CreationPipeline]].
     */
    private def create(
      authenticatorExpiry: FiniteDuration,
      fingerprint: Option[String]
    )(
      implicit
      ec: ExecutionContext
    ): CreationPipeline = {
      CreationPipeline((loginInfo: LoginInfo, payload: Option[JObject]) => {
        idGenerator.generate.map { id =>
          val now = clock.instant()
          Authenticator(
            id,
            loginInfo,
            lastUsedDateTime = now,
            expirationDateTime = now + authenticatorExpiry,
            fingerprint,
            payload
          )
        }
      })
    }
  }
}
