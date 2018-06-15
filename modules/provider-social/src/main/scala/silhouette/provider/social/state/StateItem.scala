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

import com.typesafe.scalalogging.LazyLogging
import io.circe.Json
import io.circe.parser.parse
import silhouette.crypto.Base64

/**
 * An item which can be a part of a social state.
 *
 * The social state consists of one state item per handler. So this item describes the state
 * an handler can handle. A state item can be of any type. The handler to which the state item
 * pertains, must be able to serialize/deserialize this state item.
 */
trait StateItem

/**
 * The companion object of the [[StateItem]].
 */
object StateItem {

  /**
   * A class which represents the serialized structure of a social state item.
   *
   * @param id   A unique identifier for the state item.
   * @param data The state item data as JSON.
   */
  case class ItemStructure(id: String, data: Json) {

    /**
     * Returns the serialized representation of the item.
     *
     * @return The serialized representation of the item.
     */
    def asString: String = s"${Base64.encode(id)}-${Base64.encode(data.noSpaces)}"
  }

  /**
   * The companion object of the [[ItemStructure]].
   */
  object ItemStructure extends LazyLogging {

    /**
     * An extractor which unserializes a state item from a string.
     *
     * @param str The string to unserialize.
     * @return Some [[ItemStructure]] instance on success, None on failure.
     */
    def unapply(str: String): Option[ItemStructure] = {
      str.split('-').toList match {
        case List(id, data) =>
          parse(Base64.decode(data)).fold(
            error => {
              logger.info(error.message, error)
              None
            },
            json => Some(ItemStructure(Base64.decode(id), json))
          )
        case _ => None
      }
    }
  }
}
