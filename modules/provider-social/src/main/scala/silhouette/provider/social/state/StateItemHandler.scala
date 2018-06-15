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

import silhouette.http.{ RequestPipeline, ResponsePipeline }
import silhouette.provider.social.state.StateItem.ItemStructure

import scala.concurrent.{ ExecutionContext, Future }

/**
 * Handles state for different purposes.
 */
trait StateItemHandler {

  /**
   * The item the handler can handle.
   */
  type Item <: StateItem

  /**
   * Gets the state item the handler can handle.
   *
   * @param ec The execution context to handle the asynchronous operations.
   * @return The state params the handler can handle.
   */
  def item(implicit ec: ExecutionContext): Future[Item]

  /**
   * Indicates if a handler can handle the given [[StateItem]].
   *
   * This method should check if the [[serialize]] method of this handler can serialize the given
   * unserialized state item.
   *
   * @param item The item to check for.
   * @return `Some[Item]` casted state item if the handler can handle the given state item, `None` otherwise.
   */
  def canHandle(item: StateItem): Option[Item]

  /**
   * Indicates if a handler can handle the given unserialized state item.
   *
   * This method should check if the [[unserialize]] method of this handler can unserialize the given
   * serialized state item.
   *
   * @param item    The item to check for.
   * @param request The request instance to get additional data to validate against.
   * @tparam R The type of the request.
   * @return True if the handler can handle the given state item, false otherwise.
   */
  def canHandle[R](item: ItemStructure)(implicit request: RequestPipeline[R]): Boolean

  /**
   * Returns a serialized value of the state item.
   *
   * @param item The state item to serialize.
   * @return The serialized state item.
   */
  def serialize(item: Item): ItemStructure

  /**
   * Unserializes the state item.
   *
   * @param item    The state item to unserialize.
   * @param request The request instance to get additional data to validate against.
   * @param ec      The execution context to handle the asynchronous operations.
   * @tparam R The type of the request.
   * @return The unserialized state item.
   */
  def unserialize[R](item: ItemStructure)(
    implicit
    request: RequestPipeline[R],
    ec: ExecutionContext
  ): Future[Item]
}

/**
 * A state item handler which can publish its internal state to the client.
 *
 * Some state item handlers, like the CSRF state handler, needs the ability to publish state to a cookie.
 * So if you have such a state item handler, then mixin this trait, to publish the state item to the client.
 */
trait PublishableStateItemHandler {
  self: StateItemHandler =>

  /**
   * Publishes the state to the client.
   *
   * @param item     The item to publish.
   * @param response The response to send to the client.
   * @param request  The current request.
   * @tparam R The type of the request.
   * @tparam P The type of the response.
   * @return The response to send to the client.
   */
  def publish[R, P](item: Item, response: ResponsePipeline[P])(
    implicit
    request: RequestPipeline[R]
  ): ResponsePipeline[P]
}
