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
import silhouette.crypto.{ SecureAsyncID, Signer }
import silhouette.http._
import silhouette.provider.social.state.StateItem
import silhouette.provider.social.state.StateItem.ItemStructure
import silhouette.provider.social.state.handler.CsrfStateItemHandler._
import silhouette.specs2.WaitPatience

import scala.concurrent.Future
import scala.util.Success

/**
 * Test case for the [[CsrfStateItemHandler]] class.
 *
 * @param ev The execution environment.
 */
class CsrfStateItemHandlerSpec(implicit ev: ExecutionEnv)
  extends Specification
  with Mockito
  with JsonMatchers
  with WaitPatience {

  "The `item` method" should {
    "return the CSRF state item" in new Context {
      csrfStateItemHandler.item must beEqualTo(csrfStateItem).awaitWithPatience
    }
  }

  "The `canHandle` method" should {
    "return the same item if it can handle the given item" in new Context {
      csrfStateItemHandler.canHandle(csrfStateItem) must beSome(csrfStateItem)
    }

    "should return `None` if it can't handle the given item" in new Context {
      val nonCsrfState = mock[StateItem].smart

      csrfStateItemHandler.canHandle(nonCsrfState) must beNone
    }
  }

  "The `canHandle` method" should {
    "return false if the give item is for another handler" in new Context {
      val nonCsrfItemStructure = mock[ItemStructure].smart
      nonCsrfItemStructure.id returns "non-csrf-item"

      implicit val request: RequestPipeline[FakeRequest] = FakeRequestPipeline()

      csrfStateItemHandler.canHandle(nonCsrfItemStructure) must beFalse
    }

    "return false if client state doesn't match the item state" in new Context {
      implicit val request: RequestPipeline[FakeRequest] = FakeRequestPipeline()
        .withCookies(cookie("invalid-token"))

      csrfStateItemHandler.canHandle(csrfItemStructure) must beFalse
    }

    "return true if it can handle the given `ItemStructure`" in new Context {
      implicit val request: RequestPipeline[FakeRequest] = FakeRequestPipeline()
        .withCookies(cookie(csrfStateItem.token))

      csrfStateItemHandler.canHandle(csrfItemStructure) must beTrue
    }
  }

  "The `serialize` method" should {
    "return a serialized value of the state item" in new Context {
      csrfStateItemHandler.serialize(csrfStateItem).asString must be equalTo csrfItemStructure.asString
    }
  }

  "The `unserialize` method" should {
    "unserialize the state item" in new Context {
      implicit val request: RequestPipeline[FakeRequest] = FakeRequestPipeline()

      csrfStateItemHandler.unserialize(csrfItemStructure) must beEqualTo(csrfStateItem).awaitWithPatience
    }
  }

  "The `publish` method" should {
    "publish the state item to the client" in new Context {
      implicit val request: RequestPipeline[FakeRequest] = FakeRequestPipeline()
      val result = csrfStateItemHandler.publish(csrfStateItem, FakeResponsePipeline())

      result.cookie(settings.cookieName) must beSome(cookie(csrfToken))
    }
  }

  /**
   * The context.
   */
  trait Context extends Scope {

    /**
     * A CSRF token.
     */
    val csrfToken = "csrfToken"

    /**
     * The secure ID implementation.
     */
    val secureID = {
      val m = mock[SecureAsyncID[String]].smart
      m.get returns Future.successful(csrfToken)
      m
    }

    /**
     * The settings.
     */
    val settings = CsrfStateSettings()

    /**
     * The signer implementation.
     *
     * The signer returns the same value as passed to the methods. This is enough for testing.
     */
    val signer = {
      val c = mock[Signer].smart
      c.sign(any) answers { p => p.asInstanceOf[String] }
      c.extract(any) answers { p => Success(p.asInstanceOf[String]) }
      c
    }

    /**
     * A CSRF state item.
     */
    val csrfStateItem = CsrfStateItem(csrfToken)

    /**
     * The serialized type of the CSRF state item.
     */
    val csrfItemStructure = ItemStructure(ID, csrfStateItem.asJson)

    /**
     * An instance of the CSRF state item handler.
     */
    val csrfStateItemHandler = new CsrfStateItemHandler(settings, secureID, signer)

    /**
     * A helper method to create a cookie.
     *
     * @param value The cookie value.
     * @return A cookie instance with the given value.
     */
    def cookie(value: String): Cookie = Cookie(
      name = settings.cookieName,
      value = signer.sign(value),
      maxAge = Some(settings.expirationTime.toSeconds.toInt),
      path = settings.cookiePath,
      domain = settings.cookieDomain,
      secure = settings.secureCookie,
      httpOnly = settings.httpOnlyCookie,
      sameSite = settings.sameSite
    )
  }
}
