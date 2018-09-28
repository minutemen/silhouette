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

import io.circe.syntax._
import org.specs2.concurrent.ExecutionEnv
import org.specs2.matcher.JsonMatchers
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import silhouette.http.{ Fake, RequestPipeline, SilhouetteRequest }
import silhouette.provider.social.state.StateItem
import silhouette.provider.social.state.StateItem.ItemStructure
import silhouette.provider.social.state.handler.UserStateItem._
import silhouette.provider.social.state.handler.UserStateItemHandler._
import silhouette.specs2.WaitPatience

/**
 * Test case for the [[UserStateItemHandler]] class.
 *
 * @param ev The execution environment.
 */
class UserStateItemHandlerSpec(implicit ev: ExecutionEnv)
  extends Specification
  with Mockito
  with JsonMatchers
  with WaitPatience {

  "The `item` method" should {
    "return the user state item" in new Context {
      userStateItemHandler.item must beEqualTo(userStateItem).awaitWithPatience
    }
  }

  "The `canHandle` method" should {
    "return the same item if it can handle the given item" in new Context {
      userStateItemHandler.canHandle(userStateItem) must beSome(userStateItem)
    }

    "should return `None` if it can't handle the given item" in new Context {
      val nonUserState = mock[StateItem].smart

      userStateItemHandler.canHandle(nonUserState) must beNone
    }
  }

  "The `canHandle` method" should {
    "return false if the give item is for another handler" in new Context {
      val nonUserItemStructure = mock[ItemStructure].smart
      nonUserItemStructure.id returns "non-user-item"

      implicit val request: RequestPipeline[SilhouetteRequest] = Fake.request

      userStateItemHandler.canHandle(nonUserItemStructure) must beFalse
    }

    "return true if it can handle the given `ItemStructure`" in new Context {
      implicit val request: RequestPipeline[SilhouetteRequest] = Fake.request

      userStateItemHandler.canHandle(userItemStructure) must beTrue
    }
  }

  "The `serialize` method" should {
    "return a serialized value of the state item" in new Context {
      userStateItemHandler.serialize(userStateItem).asString must be equalTo userItemStructure.asString
    }
  }

  "The `unserialize` method" should {
    "unserialize the state item" in new Context {
      implicit val request: RequestPipeline[SilhouetteRequest] = Fake.request

      userStateItemHandler.unserialize(userItemStructure) must beEqualTo(userStateItem).awaitWithPatience
    }
  }

  /**
   * The context.
   */
  trait Context extends Scope {

    /**
     * A user state item.
     */
    val userStateItem = UserStateItem(Map("path" -> "/login"))

    /**
     * The serialized type of the user state item.
     */
    val userItemStructure = ItemStructure(ID, userStateItem.asJson)

    /**
     * An instance of the user state item handler.
     */
    val userStateItemHandler = new UserStateItemHandler[UserStateItem](userStateItem)
  }
}
