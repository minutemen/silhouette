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

import cats.effect.IO
import org.specs2.matcher.JsonMatchers
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import silhouette.provider.social.state.handler.UserStateItem._

/**
 * Test case for the [[UserStateItemHandler]] class.
 */
class UserStateItemHandlerSpec extends Specification with JsonMatchers {

  /**
   * The context.
   */
  trait Context extends Scope {

    /**
     * A user state item.
     */
    val userStateItem = UserStateItem(Map("path" -> "/login"))

    /**
     * An instance of the user state item handler.
     */
    val userStateItemHandler = new UserStateItemHandler[IO, UserStateItem](userStateItem)
  }
}
