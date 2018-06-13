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
package silhouette.provider.social.state.handler

import io.circe.generic.semiauto.{ deriveDecoder, deriveEncoder }
import io.circe.syntax._
import io.circe.{ Decoder, Encoder }
import silhouette.http.RequestPipeline
import silhouette.provider.social.state.StateItem.ItemStructure
import silhouette.provider.social.state.handler.UserStateItemHandler._
import silhouette.provider.social.state.{ StateItem, StateItemHandler }

import scala.concurrent.{ ExecutionContext, Future }
import scala.reflect.ClassTag

/**
 * A default user state item where state is of type Map[String, String].
 */
case class UserStateItem(state: Map[String, String]) extends StateItem

/**
 * The companion object of the [[UserStateItem]].
 */
object UserStateItem {
  implicit val jsonDecoder: Decoder[UserStateItem] = deriveDecoder
  implicit val jsonEncoder: Encoder[UserStateItem] = deriveEncoder
}

/**
 * Handles user defined state.
 *
 * @param item     The user state item.
 * @param decoder  The JSON decoder.
 * @param encoder  The JSON encoder.
 * @param classTag The class tag for the user state item.
 * @tparam S The type of the user state.
 */
class UserStateItemHandler[S <: StateItem](item: S)(
  implicit
  decoder: Decoder[S],
  encoder: Encoder[S],
  classTag: ClassTag[S]
) extends StateItemHandler {

  /**
   * The item the handler can handle.
   */
  override type Item = S

  /**
   * Gets the state item the handler can handle.
   *
   * @param ec The execution context to handle the asynchronous operations.
   * @return The state params the handler can handle.
   */
  override def item(implicit ec: ExecutionContext): Future[Item] = Future.successful(item)

  /**
   * Indicates if a handler can handle the given `SocialStateItem`.
   *
   * This method should check if the [[serialize]] method of this handler can serialize the given
   * unserialized state item.
   *
   * @param item The item to check for.
   * @return `Some[Item]` casted state item if the handler can handle the given state item, `None` otherwise.
   */
  override def canHandle(item: StateItem): Option[Item] = item match {
    case i: Item => Some(i)
    case _       => None
  }

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
  override def canHandle[R](item: ItemStructure)(implicit request: RequestPipeline[R]): Boolean = item.id == ID

  /**
   * Returns a serialized value of the state item.
   *
   * @param item The state item to serialize.
   * @return The serialized state item.
   */
  override def serialize(item: Item): ItemStructure = ItemStructure(ID, item.asJson)

  /**
   * Unserializes the state item.
   *
   * @param item    The state item to unserialize.
   * @param request The request instance to get additional data to validate against.
   * @param ec      The execution context to handle the asynchronous operations.
   * @tparam R The type of the request.
   * @return The unserialized state item.
   */
  override def unserialize[R](item: ItemStructure)(
    implicit
    request: RequestPipeline[R],
    ec: ExecutionContext
  ): Future[Item] = {
    Future.fromTry(item.data.as[S].toTry)
  }
}

/**
 * The companion object.
 */
object UserStateItemHandler {

  /**
   * The ID of the state handler.
   */
  val ID = "user-state"
}
