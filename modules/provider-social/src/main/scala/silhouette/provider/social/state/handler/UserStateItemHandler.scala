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

import cats.effect.Async
import io.circe.syntax._
import io.circe.{ Decoder, Encoder, HCursor, Json }
import silhouette.http.{ RequestPipeline, ResponseWriter }
import silhouette.provider.social.state.handler.CsrfStateItemHandler.ID
import silhouette.provider.social.state.{ StateItem, StateItemHandler }

/**
 * A default user state item where state is of type Map[String, String].
 */
case class UserStateItem(state: Map[String, String]) extends StateItem

/**
 * The companion object of the [[UserStateItem]].
 */
object UserStateItem {
  implicit val jsonDecoder: Decoder[UserStateItem] = (c: HCursor) => for {
    state <- c.downField("state").as[Map[String, String]]
  } yield {
    new UserStateItem(state)
  }
  implicit val jsonEncoder: Encoder[UserStateItem] = (a: UserStateItem) => Json.obj(
    ("state", a.state.asJson)
  )
}

/**
 * Handles user defined state.
 *
 * @param item    The user state item.
 * @param decoder The JSON decoder.
 * @param encoder The JSON encoder.
 * @tparam I The type of the user state item.
 */
class UserStateItemHandler[F[_]: Async, I <: StateItem](item: I)(
  implicit
  decoder: Decoder[I],
  encoder: Encoder[I]
) extends StateItemHandler[F, I] {

  /**
   * Gets the ID of the handler.
   *
   * @return The ID of the handler.
   */
  override def id: String = ID

  /**
   * Returns the [[io.circe.Json]] representation of the state item and a function that can embed item specific
   * state into a response.
   *
   * A state item handler is able to embed some item specific state into the response. In the unserialize method
   * it can then be extracted from the request. So this method returns also a function, that can write this state
   * to a response.
   *
   * @tparam R The type of the response.
   * @return Either an error or the item serialized as [[io.circe.Json]] and a function, that is able to embed item
   *         specific state into a response pipeline.
   */
  override def serialize[R]: F[(Json, ResponseWriter[R])] =
    Async[F].pure(item.asJson -> identity)

  /**
   * Unserializes the state item.
   *
   * A state item handler is able to embed some item specific state into the response. In this method it can then be
   * extracted from the request.Therefore the request is also passed in addition to the serialized [[io.circe.Json]]
   * instance.
   *
   * @tparam R The type of the request.
   * @param json    The serialized state item as [[io.circe.Json]].
   * @param request The request instance to get additional data to validate against.
   * @return Either an error or the unserialized state item.
   */
  override def unserialize[R](json: Json, request: RequestPipeline[R]): F[I] =
    Async[F].fromEither(json.as[I])
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
