/**
 * Copyright 2015 Mohiva Organisation (license at mohiva dot com)
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
package com.mohiva.silhouette.authenticators

import com.mohiva.silhouette.http.{ RequestPipeline, ResponsePipeline }
import com.mohiva.silhouette.services.AuthenticatorService
import com.mohiva.silhouette.{ Authenticator, LoginInfo }

import scala.concurrent.{ ExecutionContext, Future }

/**
 * An authenticator that can be used if a client doesn't need an authenticator to
 * track a user. This can be useful for request providers, because authentication
 * may occur here on every request to a protected resource.
 *
 * @param loginInfo The linked login info for an identity.
 */
case class DummyAuthenticator(loginInfo: LoginInfo) extends Authenticator {

  /**
   * The Type of the generated value an authenticator will be serialized to.
   */
  override type Value = Unit

  /**
   * Authenticator is always valid.
   *
   * @return True because it's always valid.
   */
  override def isValid = true
}

/**
 * The service that handles the dummy token authenticator.
 *
 * @param executionContext The execution context to handle the asynchronous operations.
 */
class DummyAuthenticatorService(implicit val executionContext: ExecutionContext)
  extends AuthenticatorService[DummyAuthenticator] {

  /**
   * Creates a new authenticator for the specified login info.
   *
   * @param loginInfo The login info for which the authenticator should be created.
   * @param request The request pipeline.
   * @tparam R The type of the request.
   * @return An authenticator.
   */
  override def create[R](loginInfo: LoginInfo)(implicit request: RequestPipeline[R]): Future[DummyAuthenticator] = {
    Future.successful(DummyAuthenticator(loginInfo))
  }

  /**
   * Retrieves the authenticator from request.
   *
   * Doesn't need to return an authenticator here, because this method will not be called if
   * a request provider grants access. If the authentication with a request provider has failed,
   * then this method must return None to not grant access to the resource.
   *
   * @param request The request pipeline to retrieve the authenticator from.
   * @tparam R The type of the request.
   * @return Some authenticator or None if no authenticator could be found in request.
   */
  override def retrieve[R](implicit request: RequestPipeline[R]): Future[Option[DummyAuthenticator]] = {
    Future.successful(None)
  }

  /**
   * Returns noting because this authenticator doesn't have a serialized representation.
   *
   * @param authenticator The authenticator instance.
   * @param request The request pipeline.
   * @tparam R The type of the request.
   * @return The serialized authenticator value.
   */
  override def init[R](authenticator: DummyAuthenticator)(implicit request: RequestPipeline[R]): Future[Unit] = {
    Future.successful(())
  }

  /**
   * Returns the original result, because we needn't add the authenticator to the result.
   *
   * @param value The authenticator value to embed.
   * @param response The response pipeline to manipulate.
   * @param request The request pipeline.
   * @tparam R The type of the request.
   * @tparam P The type of the response.
   * @return The manipulated response pipeline.
   */
  override def embed[R, P](value: Unit, response: ResponsePipeline[P])(
    implicit request: RequestPipeline[R]): Future[ResponsePipeline[P]] = {

    Future.successful(response.touch)
  }

  /**
   * Returns the original request, because we needn't add the authenticator to the request.
   *
   * @param value The authenticator value to embed.
   * @param request The request pipeline.
   * @tparam R The type of the request.
   * @return The manipulated request pipeline.
   */
  override def embed[R](value: Unit, request: RequestPipeline[R]): RequestPipeline[R] = request

  /**
   * @inheritdoc
   * @param authenticator The authenticator to touch.
   * @return The touched authenticator on the left or the untouched authenticator on the right.
   */
  override def touch(authenticator: DummyAuthenticator): Either[DummyAuthenticator, DummyAuthenticator] = {
    Right(authenticator)
  }

  /**
   * Returns the original request, because we needn't update the authenticator in the result.
   *
   * @param authenticator The authenticator to update.
   * @param response The response pipeline to manipulate.
   * @param request The request pipeline.
   * @tparam R The type of the request.
   * @tparam P The type of the response.
   * @return The original or a manipulated response pipeline.
   */
  override def update[R, P](authenticator: DummyAuthenticator, response: ResponsePipeline[P])(
    implicit request: RequestPipeline[R]): Future[ResponsePipeline[P]] = {

    Future.successful(response.touch)
  }

  /**
   * Returns noting because this authenticator doesn't have a serialized representation.
   *
   * @param authenticator The authenticator to renew.
   * @param request The request pipeline.
   * @tparam R The type of the request.
   * @return The serialized expression of the authenticator.
   */
  override def renew[R](authenticator: DummyAuthenticator)(implicit request: RequestPipeline[R]): Future[Unit] = {
    Future.successful(())
  }

  /**
   * Returns the original request, because we needn't renew the authenticator in the result.
   *
   * @param authenticator The authenticator to renew.
   * @param response The response pipeline to manipulate.
   * @param request The request pipeline.
   * @tparam R The type of the request.
   * @tparam P The type of the response.
   * @return The original or a manipulated response pipeline.
   */
  override def renew[R, P](authenticator: DummyAuthenticator, response: ResponsePipeline[P])(
    implicit request: RequestPipeline[R]): Future[ResponsePipeline[P]] = {

    Future.successful(response.touch)
  }

  /**
   * Returns the original request, because we needn't discard the authenticator in the result.
   *
   * @param authenticator The authenticator instance.
   * @param response The response pipeline to manipulate.
   * @param request The request pipeline.
   * @tparam R The type of the request.
   * @tparam P The type of the response.
   * @return The manipulated response pipeline.
   */
  override def discard[R, P](authenticator: DummyAuthenticator, response: ResponsePipeline[P])(
    implicit request: RequestPipeline[R]): Future[ResponsePipeline[P]] = {

    Future.successful(response.touch)
  }
}

/**
 * The companion object of the authenticator service.
 */
object DummyAuthenticatorService {

  /**
   * The ID of the authenticator.
   */
  val ID = "dummy-authenticator"
}
