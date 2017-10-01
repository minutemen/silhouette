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
package silhouette.services

import silhouette.http.{ RequestPipeline, ResponsePipeline }
import silhouette.{ Authenticator, ExecutionContextProvider, LoginInfo }

import scala.concurrent.Future
import scala.json.ast.JObject

/**
 * A pipeline which tries to transforms a request into an authenticator.
 *
 * @param pipeline The transformation pipeline.
 * @tparam R The type of the request.
 */
case class RetrievalPipeline[R](pipeline: RequestPipeline[R] => Future[Option[Authenticator]])

/**
 * Handles authenticators for the Silhouette library.
 */
trait AuthenticatorService extends ExecutionContextProvider {

  /**
   * Creates a new authenticator for the specified login info.
   *
   * @param loginInfo The login info for which the authenticator should be created.
   * @param payload   Some custom payload an authenticator can transport.
   * @param request   The request pipeline.
   * @tparam R The type of the request.
   * @return An authenticator.
   */
  def create[R](loginInfo: LoginInfo, payload: Option[JObject] = None)(
    implicit
    request: RequestPipeline[R]
  ): Future[Authenticator]

  /**
   * Retrieves the authenticator from request.
   *
   * An authenticator can be located in different parts of the request and by specifying different
   * request transports it's possible to retrieve the authenticator from this different request
   * parts.
   *
   * A transport itself extracts the authenticator related artefacts from the request as a string. But
   * With the help of a [[silhouette.authenticator.AuthenticatorFormat]], it's possible to transform
   * this string into an [[Authenticator]]. Formats are composable transformers. This means a transformation
   * from a string into an [[Authenticator]] can run through a chain of different transformers, each of
   * them applying a different transformation.
   *
   * By combining the transports with the authenticator transformation pipeline, it makes it now possible
   * to extract different authenticator types from different request parts.
   *
   * The retrieval pipeline is such a transformation pipeline. This method can accept multiple retrieval
   * pipeline, where each tries to transform a request into an authenticator.If multiple pipelines are given,
   * then the first found authenticator will be used.
   *
   * @param retrievalPipeline The retrieval pipeline which tries to transforms the request into an authenticator.
   * @tparam R The type of the request.
   * @return Some authenticator or None if no authenticator could be found in request.
   */
  def retrieve[R](retrievalPipeline: RetrievalPipeline[R]*)(
    implicit
    request: RequestPipeline[R]
  ): Future[Option[Authenticator]]

  /**
   * Embeds an authenticator into the response.
   *
   * @param authenticator The authenticator to embed.
   * @param response      The response pipeline to manipulate.
   * @param request       The request pipeline.
   * @tparam R The type of the request.
   * @tparam P The type of the response.
   * @return The manipulated response pipeline.
   */
  def embed[R, P](authenticator: Authenticator, response: ResponsePipeline[P])(
    implicit
    request: RequestPipeline[R]
  ): Future[ResponsePipeline[P]]

  /**
   * Touches an authenticator.
   *
   * An authenticator can use sliding window expiration. This means that the authenticator times
   * out after a certain time if it wasn't used. So to mark an authenticator as used it will be
   * touched on every request to a Silhouette action. If an authenticator should not be touched
   * because of the fact that sliding window expiration is disabled, then it should be returned
   * on the right, otherwise it should be returned on the left. An untouched authenticator needn't
   * be updated later by the [[update]] method.
   *
   * @param authenticator The authenticator to touch.
   * @return The touched authenticator on the left or the untouched authenticator on the right.
   */
  def touch(authenticator: Authenticator): Either[Authenticator, Authenticator]

  /**
   * Updates a touched authenticator.
   *
   * If the authenticator was updated, then the updated artifacts should be embedded into the response.
   * This method gets called on every subsequent request if an identity accesses a Silhouette action,
   * expect the authenticator was not touched.
   *
   * @param authenticator The authenticator to update.
   * @param response      The response pipeline to manipulate.
   * @param request       The request pipeline.
   * @tparam R The type of the request.
   * @tparam P The type of the response.
   * @return The original or a manipulated response pipeline.
   */
  def update[R, P](authenticator: Authenticator, response: ResponsePipeline[P])(
    implicit
    request: RequestPipeline[R]
  ): Future[ResponsePipeline[P]]

  /**
   * Renews the expiration of an authenticator without embedding it into the response.
   *
   * Based on the implementation, the renew method should revoke the given authenticator first, before
   * creating a new one. If the authenticator was updated, then the updated artifacts should be returned.
   *
   * @param authenticator The authenticator to renew.
   * @param request       The request pipeline.
   * @tparam R The type of the request.
   * @return The renewed authenticator.
   */
  def renew[R](authenticator: Authenticator)(implicit request: RequestPipeline[R]): Future[Authenticator]

  /**
   * Renews the expiration of an authenticator.
   *
   * Based on the implementation, the renew method should revoke the given authenticator first, before
   * creating a new one. If the authenticator was updated, then the updated artifacts should be embedded
   * into the response.
   *
   * @param authenticator The authenticator to renew.
   * @param response      The response pipeline to manipulate.
   * @param request       The request pipeline.
   * @tparam R The type of the request.
   * @tparam P The type of the response.
   * @return The original or a manipulated response pipeline.
   */
  def renew[R, P](authenticator: Authenticator, response: ResponsePipeline[P])(
    implicit
    request: RequestPipeline[R]
  ): Future[ResponsePipeline[P]]

  /**
   * Manipulates the response and removes authenticator specific artifacts before sending it to the client.
   *
   * @param authenticator The authenticator instance.
   * @param response      The response pipeline to manipulate.
   * @param request       The request pipeline.
   * @tparam R The type of the request.
   * @tparam P The type of the response.
   * @return The manipulated response pipeline.
   */
  def discard[R, P](authenticator: Authenticator, response: ResponsePipeline[P])(
    implicit
    request: RequestPipeline[R]
  ): Future[ResponsePipeline[P]]
}

/**
 * The companion object.
 */
object AuthenticatorService {

  /**
   * The error messages.
   */
  val CreateError = "[Silhouette][%s] Could not create authenticator for login info: %s"
  val RetrieveError = "[Silhouette][%s] Could not retrieve authenticator"
  val InitError = "[Silhouette][%s] Could not init authenticator: %s"
  val UpdateError = "[Silhouette][%s] Could not update authenticator: %s"
  val RenewError = "[Silhouette][%s] Could not renew authenticator: %s"
  val DiscardError = "[Silhouette][%s] Could not discard authenticator: %s"
}
