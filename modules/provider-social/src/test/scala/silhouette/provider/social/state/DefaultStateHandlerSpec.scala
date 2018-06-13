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

import io.circe.Json
import org.specs2.concurrent.ExecutionEnv
import org.specs2.matcher.JsonMatchers
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import silhouette.crypto.Signer
import silhouette.http.{ FakeRequest, FakeRequestPipeline, FakeResponsePipeline, RequestPipeline }
import silhouette.provider.ProviderException
import silhouette.provider.social.state.DefaultStateHandler._
import silhouette.provider.social.state.StateItem.ItemStructure
import silhouette.specs2.WaitPatience

import scala.concurrent.Future
import scala.util.{ Failure, Success }

/**
 * Test case for the [[DefaultStateHandler]] class.
 *
 * @param ev The execution environment.
 */
class DefaultStateHandlerSpec(implicit ev: ExecutionEnv)
  extends Specification
  with Mockito
  with JsonMatchers
  with WaitPatience {

  "The `withHandler` method" should {
    "return a new state handler with the given item handler added" in new Context {
      val newHandler = mock[StateItemHandler].smart

      stateHandler.handlers.size must be equalTo 2
      stateHandler.withHandler(newHandler).handlers.size must be equalTo 3
    }
  }

  "The `state` method" should {
    "return the social state" in new Context {
      Default.itemHandler.item returns Future.successful(Default.item)
      Publishable.itemHandler.item returns Future.successful(Publishable.item)

      stateHandler.state must beEqualTo(state).awaitWithPatience
    }
  }

  "The `serialize` method" should {
    "return an empty string if no handler is registered" in new Context {
      override val stateHandler = new DefaultStateHandler(Set(), signer)

      stateHandler.serialize(state) must be equalTo ""
    }

    "return an empty string if the items are empty" in new Context {
      stateHandler.serialize(State(Set())) must be equalTo ""
    }

    "return the serialized social state" in new Context {
      Default.itemHandler.canHandle(Publishable.item) returns None
      Default.itemHandler.canHandle(Default.item) returns Some(Default.item)
      Default.itemHandler.serialize(Default.item) returns Default.structure

      Publishable.itemHandler.canHandle(Default.item) returns None
      Publishable.itemHandler.canHandle(Publishable.item) returns Some(Publishable.item)
      Publishable.itemHandler.serialize(Publishable.item) returns Publishable.structure

      stateHandler.serialize(state) must be equalTo s"${Publishable.structure.asString}.${Default.structure.asString}"
    }
  }

  "The `unserialize` method" should {
    "omit state validation if no handler is registered" in new Context {
      override val stateHandler = new DefaultStateHandler(Set(), signer)
      implicit val request: RequestPipeline[FakeRequest] = FakeRequestPipeline()

      stateHandler.unserialize("") must beEqualTo(State(Set())).awaitWithPatience

      there was no(signer).extract(any[String])
    }

    "throw an Exception for an empty string" in new Context {
      implicit val request: RequestPipeline[FakeRequest] = FakeRequestPipeline()

      stateHandler.unserialize("") must throwA[RuntimeException].like {
        case e =>
          e.getMessage must startWith("Wrong state format")
      }.awaitWithPatience
    }

    "throw an ProviderException if the serialized item structure cannot be extracted" in new Context {
      implicit val request: RequestPipeline[FakeRequest] = FakeRequestPipeline()
      val serialized = s"some-wired-content"

      stateHandler.unserialize(serialized) must throwA[ProviderException].like {
        case e =>
          e.getMessage must startWith(ItemExtractionError.format(serialized))
      }.awaitWithPatience
    }

    "throw an ProviderException if none of the item handlers can handle the given state" in new Context {
      implicit val request: RequestPipeline[FakeRequest] = FakeRequestPipeline()
      val serialized = s"${Default.structure.asString}"

      Default.itemHandler.canHandle(any[ItemStructure])(any) returns false
      Publishable.itemHandler.canHandle(any[ItemStructure])(any) returns false

      stateHandler.unserialize(serialized) must throwA[ProviderException].like {
        case e =>
          e.getMessage must startWith(MissingItemHandlerError.format(Default.structure))
      }.awaitWithPatience
    }

    "return the unserialized social state" in new Context {
      implicit val request: RequestPipeline[FakeRequest] = FakeRequestPipeline()
      val serialized = s"${Default.structure.asString}.${Publishable.structure.asString}"

      Default.itemHandler.canHandle(Publishable.structure) returns false
      Default.itemHandler.canHandle(Default.structure) returns true
      Default.itemHandler.unserialize(Default.structure) returns Future.successful(Default.item)

      Publishable.itemHandler.canHandle(Default.structure) returns false
      Publishable.itemHandler.canHandle(Publishable.structure) returns true
      Publishable.itemHandler.unserialize(Publishable.structure) returns Future.successful(Publishable.item)

      stateHandler.unserialize(serialized) must beEqualTo(State(Set(Default.item, Publishable.item))).awaitWithPatience
    }
  }

  "The `publish` method" should {
    "should publish the state with the publishable handler that is responsible for the item" in new Context {
      implicit val request: RequestPipeline[FakeRequest] = FakeRequestPipeline()

      val response = FakeResponsePipeline()
      val publishedResult = FakeResponsePipeline().withHeaders("X-PUBLISHED" -> "true")

      Publishable.itemHandler.publish(Publishable.item, response) returns publishedResult
      Publishable.itemHandler.canHandle(Default.item) returns None
      Publishable.itemHandler.canHandle(Publishable.item) returns Some(Publishable.item)

      stateHandler.publish(response, state) must be equalTo publishedResult
    }

    "should not publish the state if no publishable handler is responsible" in new Context {
      implicit val request: RequestPipeline[FakeRequest] = FakeRequestPipeline()
      val response = FakeResponsePipeline()

      Publishable.itemHandler.canHandle(Default.item) returns None
      Publishable.itemHandler.canHandle(Publishable.item) returns None

      stateHandler.publish(response, state) must be equalTo response
    }
  }

  /**
   * The context.
   */
  trait Context extends Scope {

    /**
     * A default handler implementation.
     */
    case class DefaultItem() extends StateItem
    trait DefaultItemHandler extends StateItemHandler {
      type Item = DefaultItem
    }
    object Default {
      val itemHandler = mock[DefaultItemHandler].smart
      val item = DefaultItem()
      val structure = ItemStructure("default", Json.obj())
    }

    /**
     * A publishable handler implementation.
     */
    case class PublishableItem() extends StateItem
    trait PublishableItemHandler extends StateItemHandler with PublishableStateItemHandler {
      type Item = PublishableItem
    }
    object Publishable {
      val itemHandler = mock[PublishableItemHandler].smart
      val item = PublishableItem()
      val structure = ItemStructure("publishable", Json.obj())
    }

    /**
     * The signer implementation.
     *
     * The signer returns the same value as passed to the methods. This is enough for testing.
     */
    val signer = {
      val c = mock[Signer].smart
      c.sign(any) answers { p => p.asInstanceOf[String] }
      c.extract(any) answers { p =>
        p.asInstanceOf[String] match {
          case "" => Failure(new RuntimeException("Wrong state format"))
          case s  => Success(s)
        }
      }
      c
    }

    /**
     * The state.
     */
    val state = State(Set(Publishable.item, Default.item))

    /**
     * The state handler to test.
     */
    val stateHandler = new DefaultStateHandler(Set(Publishable.itemHandler, Default.itemHandler), signer)
  }
}
