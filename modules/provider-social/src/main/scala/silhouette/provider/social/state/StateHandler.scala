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
package silhouette.provider.social.state

import silhouette.crypto.Signer
import silhouette.http.{ RequestPipeline, ResponsePipeline }
import silhouette.provider.social.SocialProviderException
import silhouette.provider.social.state.DefaultStateHandler._
import silhouette.provider.social.state.StateItem.ItemStructure

import scala.concurrent.{ ExecutionContext, Future }

/**
 * Provides a way to handle different types of state for providers that allow a state param.
 *
 * Some authentication protocols defines a state param which can be used to transport some
 * state to an authentication provider. The authentication provider sends this state back
 * to the application, after the authentication to the provider was granted.
 *
 * The state parameter can be used for different things. Silhouette provides two state handlers
 * out of the box. One state handler can transport additional user state to the provider. This
 * could be an URL were the user should be redirected after authentication to the provider, or
 * any other per-authentication based state. An other important state handler protects the
 * application for CSRF attacks.
 */
trait StateHandler {

  /**
   * The concrete instance of the state handler.
   */
  type Self <: StateHandler

  /**
   * The item handlers configured for this handler
   */
  val handlers: Set[StateItemHandler]

  /**
   * Creates a copy of the state provider with a new handler added.
   *
   * There exists two types of state handlers. The first type are global state handlers which can be configured
   * by the user with the help of a configuration mechanism or through dependency injection. And there a local
   * state handlers which are provided by the application itself. This method exists to handle the last type of
   * state handlers, because it allows to extend the list of user defined state handlers from inside the library.
   *
   * @param handler The handler to add.
   * @return A new state provider with a new handler added.
   */
  def withHandler(handler: StateItemHandler): Self

  /**
   * Gets the social state for all handlers.
   *
   * @return The social state for all handlers.
   */
  def state: Future[State]

  /**
   * Serializes the given state into a single state value which can be passed with the state param.
   *
   * @param state The social state to serialize.
   * @return Some serialized state as string if a state handler was registered and if a state item is available,
   *         None otherwise.
   */
  def serialize(state: State): Option[String]

  /**
   * Unserializes the social state from the state param.
   *
   * @param state   The state to unserialize.
   * @param request The request to read the value of the state param from.
   * @tparam R The type of the request.
   * @return The social state on success, an error on failure.
   */
  def unserialize[R](state: String)(implicit request: RequestPipeline[R]): Future[State]

  /**
   * Publishes the state to the client.
   *
   * @param response The response to send to the client.
   * @param state    The state to publish.
   * @param request  The current request.
   * @tparam R The type of the request.
   * @tparam P The type of the response.
   * @return The response to send to the client.
   */
  def publish[R, P](response: ResponsePipeline[P], state: State)(
    implicit
    request: RequestPipeline[R]
  ): ResponsePipeline[P]
}

/**
 * The base implementation of the [[StateHandler]].
 *
 * @param handlers The item handlers configured for this handler.
 * @param signer   The signer implementation to sign the state.
 * @param ec       The execution context to handle the asynchronous operations.
 */
class DefaultStateHandler(val handlers: Set[StateItemHandler], signer: Signer)(implicit ec: ExecutionContext)
  extends StateHandler {

  /**
   * The concrete instance of the state provider.
   */
  override type Self = DefaultStateHandler

  /**
   * Creates a copy of the state provider with a new handler added.
   *
   * There exists two types of state handlers. The first type are global state handlers which can be configured
   * by the user with the help of a configuration mechanism or through dependency injection. And there a local
   * state handlers which are provided by the application itself. This method exists to handle the last type of
   * state handlers, because it allows to extend the list of user defined state handlers from inside the library.
   *
   * @param handler The handler to add.
   * @return A new state provider with a new handler added.
   */
  override def withHandler(handler: StateItemHandler): DefaultStateHandler = {
    new DefaultStateHandler(handlers + handler, signer)
  }

  /**
   * Gets the social state for all handlers.
   *
   * @return The social state for all handlers.
   */
  override def state: Future[State] = {
    Future.sequence(handlers.map(_.item)).map(items => State(items.toSet))
  }

  /**
   * Serializes the given state into a single state value which can be passed with the state param.
   *
   * If no handler is registered on the provider or if no state items are available, then we omit state signing,
   * because it makes no sense the sign an empty state.
   *
   * @param state The social state to serialize.
   * @return Some serialized state as string if a state handler was registered and if a state item is available,
   *         None otherwise.
   */
  override def serialize(state: State): Option[String] = {
    if (handlers.isEmpty || state.items.isEmpty) {
      None
    } else {
      Some(signer.sign(state.items.flatMap { i =>
        handlers.flatMap(h => h.canHandle(i).map(h.serialize)).map(_.asString)
      }.mkString(".")))
    }
  }

  /**
   * Unserializes the social state from the state param.
   *
   * If no handler is registered on the provider then we omit the state validation. This is needed in some cases
   * where the authentication process was started from a client side library and not from Silhouette.
   *
   * @param state   The state to unserialize.
   * @param request The request to read the value of the state param from.
   * @tparam R The type of the request.
   * @return The social state on success, an error on failure.
   */
  override def unserialize[R](state: String)(implicit request: RequestPipeline[R]): Future[State] = {
    if (handlers.isEmpty) {
      Future.successful(State(Set()))
    } else {
      Future.fromTry(signer.extract(state)).flatMap { state =>
        state.split('.').toList match {
          case Nil | List("") =>
            Future.successful(State(Set()))
          case items =>
            Future.sequence {
              items.map {
                case ItemStructure(item) => handlers.find(_.canHandle(item)) match {
                  case Some(handler) => handler.unserialize(item)
                  case None          => Future.failed(new SocialProviderException(MissingItemHandlerError.format(item)))
                }
                case item => Future.failed(new SocialProviderException(ItemExtractionError.format(item)))
              }
            }.map(items => State(items.toSet))
        }
      }
    }
  }

  /**
   * Publishes the state to the client.
   *
   * @param response The response to send to the client.
   * @param state    The state to publish.
   * @param request  The current request.
   * @tparam R The type of the request.
   * @tparam P The type of the response.
   * @return The response to send to the client.
   * @see [[PublishableStateItemHandler]]
   */
  override def publish[R, P](response: ResponsePipeline[P], state: State)(
    implicit
    request: RequestPipeline[R]
  ): ResponsePipeline[P] = {
    handlers.collect { case h: PublishableStateItemHandler => h }.foldLeft(response) { (r, handler) =>
      state.items.foldLeft(r) { (r, item) =>
        handler.canHandle(item).map(item => handler.publish(item, r)).getOrElse(r)
      }
    }
  }
}

/**
 * The companion object for the [[DefaultStateHandler]] class.
 */
object DefaultStateHandler {

  /**
   * Some errors.
   */
  val MissingItemHandlerError = "None of the registered handlers can handle the given state item: %s"
  val ItemExtractionError = "Cannot extract social state item from string: %s"
}
