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
package silhouette.provider

import cats.data.NonEmptyList
import com.typesafe.scalalogging.LazyLogging
import silhouette._
import silhouette.http.{ RequestPipeline, ResponsePipeline }

/**
 * A provider which can be hooked into a request.
 *
 * It scans the request for credentials and returns a [[silhouette.http.ResponsePipeline]] for an [[AuthState]].
 *
 * @tparam F The type of the IO monad.
 * @tparam R The type of the request.
 * @tparam P The type of the response.
 * @tparam I The type of the identity.
 */
trait RequestProvider[F[_], R, P, I <: Identity] extends Provider {

  /**
   * The type of the credentials.
   */
  type C <: Credentials

  /**
   * Handles an [[AuthState]] and returns a [[http.ResponsePipeline]].
   */
  type AuthStateHandler = AuthState[I, C] => F[ResponsePipeline[P]]

  /**
   * Authenticates an identity based on credentials sent in a request.
   *
   * @param request The request pipeline.
   * @param handler A function that returns a [[http.ResponsePipeline]] for the given [[AuthState]].
   * @return The [[http.ResponsePipeline]].
   */
  def authenticate(request: RequestPipeline[R])(handler: AuthStateHandler): F[ResponsePipeline[P]]
}

/**
 * An implementation of the [[RequestProvider]] interface which authenticates against a list of request providers.
 *
 * @param providers The list of request providers to try to authenticate against.
 * @tparam F The type of the IO monad.
 * @tparam R The type of the request.
 * @tparam P The type of the response.
 * @tparam I The type of the identity.
 */
case class RequestProviders[F[_], R, P, I <: Identity](providers: NonEmptyList[RequestProvider[F, R, P, I]])
  extends RequestProvider[F, R, P, I] with LazyLogging {

  /**
   * The type of the credentials.
   */
  override type C = Credentials

  /**
   * Authenticates an identity based on credentials sent in a request.
   *
   * This function recursively iterates over the given list of request providers.
   *
   * The method authenticates a request against a list of several request providers. If a request provider from the
   * list is able to authenticate the request, then it calls the handler with the [[Authenticated]] state. If none of
   * the handlers is able to authenticate the request, then it calls the handler with the [[Unauthenticated]] state.
   *
   * @param request The request pipeline.
   * @param handler A function that returns a [[http.ResponsePipeline]] for the given [[AuthState]].
   * @return The [[http.ResponsePipeline]].
   */
  override def authenticate(request: RequestPipeline[R])(handler: AuthStateHandler): F[ResponsePipeline[P]] = {
    def auth(head: RequestProvider[F, R, P, I], tail: List[RequestProvider[F, R, P, I]]): F[ResponsePipeline[P]] = {
      head.authenticate(request) {
        case state: Authenticated[I, C] => handler(state)
        case state: Unauthenticated[I, C] =>
          state match {
            case MissingCredentials() =>
              logger.info(s"Couldn't find credentials for provider ${head.id}")
            case InvalidCredentials(credentials, errors) =>
              logger.info(s"Invalid credentials $credentials for provider ${head.id}; got errors: $errors")
            case MissingIdentity(_, loginInfo) =>
              logger.info(s"Couldn't find identity for login info: $loginInfo")
            case AuthFailure(cause) =>
              logger.info("Error during authentication process", cause)
          }

          tail match {
            case Nil    => handler(state)
            case h :: t => auth(h, t)
          }
      }
    }

    auth(providers.head, providers.tail)
  }

  /**
   * Gets the provider ID.
   *
   * @return The provider ID.
   */
  override def id: String = providers.toList.map(_.id).mkString(",")
}
