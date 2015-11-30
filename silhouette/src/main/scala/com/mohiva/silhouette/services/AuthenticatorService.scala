/**
 * Original work: SecureSocial (https://github.com/jaliss/securesocial)
 * Copyright 2013 Jorge Aliss (jaliss at gmail dot com) - twitter: @jaliss
 *
 * Derivative work: Silhouette (https://github.com/mohiva/silhouette)
 * Modifications Copyright 2015 Mohiva Organisation (license at mohiva dot com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mohiva.silhouette.services

import com.mohiva.silhouette._
import com.mohiva.silhouette.http.{ ResponsePipeline, RequestPipeline }

import scala.concurrent.Future

/**
 * Handles authenticators for the Silhouette library.
 *
 * @tparam T The type of the authenticator this service is responsible for.
 */
trait AuthenticatorService[T <: Authenticator] extends ExecutionContextProvider {

  /**
   * Creates a new authenticator for the specified login info.
   *
   * @param loginInfo The login info for which the authenticator should be created.
   * @param request The request pipeline.
   * @tparam R The type of the request.
   * @return An authenticator.
   */
  def create[R](loginInfo: LoginInfo)(implicit request: RequestPipeline[R]): Future[T]

  /**
   * Retrieves the authenticator from request.
   *
   * @param request The request pipeline to retrieve the authenticator from.
   * @tparam R The type of the request.
   * @return Some authenticator or None if no authenticator could be found in request.
   */
  def retrieve[R](implicit request: RequestPipeline[R]): Future[Option[T]]

  /**
   * Initializes an authenticator and instead of embedding into the the request or response, it returns
   * the serialized value.
   *
   * @param authenticator The authenticator instance.
   * @param request The request pipeline.
   * @tparam R The type of the request.
   * @return The serialized authenticator value.
   */
  def init[R](authenticator: T)(implicit request: RequestPipeline[R]): Future[T#Value]

  /**
   * Embeds authenticator specific artifacts into the response.
   *
   * @param value The authenticator value to embed.
   * @param response The response pipeline to manipulate.
   * @param request The request pipeline.
   * @tparam R The type of the request.
   * @tparam P The type of the response.
   * @return The manipulated response pipeline.
   */
  def embed[R, P](value: T#Value, response: ResponsePipeline[P])(implicit request: RequestPipeline[R]): Future[ResponsePipeline[P]]

  /**
   * Embeds authenticator specific artifacts into the request.
   *
   * This method can be used to embed an authenticator in a existing request. This can be useful
   * for testing. So before accessing a endpoint we can embed the authenticator in the request
   * to lead the action to believe that the request is a new request which contains a valid
   * authenticator.
   *
   * If an existing authenticator exists, then it will be overridden.
   *
   * @param value The authenticator value to embed.
   * @param request The request pipeline.
   * @tparam R The type of the request.
   * @return The manipulated request pipeline.
   */
  def embed[R](value: T#Value, request: RequestPipeline[R]): RequestPipeline[R]

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
  def touch(authenticator: T): Either[T, T]

  /**
   * Updates a touched authenticator.
   *
   * If the authenticator was updated, then the updated artifacts should be embedded into the response.
   * This method gets called on every subsequent request if an identity accesses a Silhouette action,
   * expect the authenticator was not touched.
   *
   * @param authenticator The authenticator to update.
   * @param response The response pipeline to manipulate.
   * @param request The request pipeline.
   * @tparam R The type of the request.
   * @tparam P The type of the response.
   * @return The original or a manipulated response pipeline.
   */
  def update[R, P](authenticator: T, response: ResponsePipeline[P])(implicit request: RequestPipeline[R]): Future[ResponsePipeline[P]]

  /**
   * Renews the expiration of an authenticator without embedding it into the response.
   *
   * Based on the implementation, the renew method should revoke the given authenticator first, before
   * creating a new one. If the authenticator was updated, then the updated artifacts should be returned.
   *
   * @param authenticator The authenticator to renew.
   * @param request The request pipeline.
   * @tparam R The type of the request.
   * @return The serialized expression of the authenticator.
   */
  def renew[R](authenticator: T)(implicit request: RequestPipeline[R]): Future[T#Value]

  /**
   * Renews the expiration of an authenticator.
   *
   * Based on the implementation, the renew method should revoke the given authenticator first, before
   * creating a new one. If the authenticator was updated, then the updated artifacts should be embedded
   * into the response.
   *
   * @param authenticator The authenticator to renew.
   * @param response The response pipeline to manipulate.
   * @param request The request pipeline.
   * @tparam R The type of the request.
   * @tparam P The type of the response.
   * @return The original or a manipulated response pipeline.
   */
  def renew[R, P](authenticator: T, response: ResponsePipeline[P])(implicit request: RequestPipeline[R]): Future[ResponsePipeline[P]]

  /**
   * Manipulates the response and removes authenticator specific artifacts before sending it to the client.
   *
   * @param authenticator The authenticator instance.
   * @param response The response pipeline to manipulate.
   * @param request The request pipeline.
   * @tparam R The type of the request.
   * @tparam P The type of the response.
   * @return The manipulated response pipeline.
   */
  def discard[R, P](authenticator: T, response: ResponsePipeline[P])(implicit request: RequestPipeline[R]): Future[ResponsePipeline[P]]
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
