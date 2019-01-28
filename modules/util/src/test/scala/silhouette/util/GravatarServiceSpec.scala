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
package silhouette.util

import org.specs2.concurrent.ExecutionEnv
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import silhouette.http.client.{ Request, Response }
import silhouette.http.{ HttpClient, Status }
import silhouette.specs2.WaitPatience
import silhouette.util.GravatarService._

import scala.concurrent.Future

/**
 * Test case for the [[GravatarService]] class.
 *
 * @param ev The execution environment.
 */
class GravatarServiceSpec(implicit ev: ExecutionEnv) extends Specification with Mockito with WaitPatience {

  "The `retrieveURI` method" should {
    "return None if email is empty" in new Context {
      service.retrieveURI("") should beNone.awaitWithPatience
    }

    "return None if HTTP status code isn't 200" in new Context {
      response.status returns Status.`Not Found`

      service.retrieveURI(email) should beNone.awaitWithPatience
    }

    "return None if HTTP status code isn't 200" in new Context {
      response.status returns Status.`Not Found`

      service.retrieveURI(email) should beNone.awaitWithPatience
    }

    "return None if exception will be thrown during API request" in new Context {
      response.status throws new RuntimeException("Timeout error")

      service.retrieveURI(email) should beNone.awaitWithPatience
    }

    "return secure Avatar URI" in new Context {
      response.status returns Status.OK

      service.retrieveURI(email) should beSome(SecureURI.format(hash, "?d=404").toURI).awaitWithPatience
    }

    "return insecure Avatar URI" in new Context {
      config.secure returns false
      response.status returns Status.OK

      service.retrieveURI(email) should beSome(InsecureURI.format(hash, "?d=404").toURI).awaitWithPatience
    }

    "return an URI with additional parameters" in new Context {
      config.params returns Map("d" -> "http://example.com/images/avatar.jpg", "s" -> "400")
      response.status returns Status.OK

      service.retrieveURI(email) should beSome(
        SecureURI.format(hash, "?d=http%3A%2F%2Fexample.com%2Fimages%2Favatar.jpg&s=400").toURI
      ).awaitWithPatience
    }

    "not trim leading zeros" in new Context {
      response.status returns Status.OK

      service.retrieveURI("123test@test.com") should beSome(
        SecureURI.format("0d77aed6b4c5857473c9a04c2017f8b8", "?d=404").toURI
      ).awaitWithPatience
    }
  }

  /**
   * The context.
   */
  trait Context extends Scope {

    /**
     * The HTTP response mock.
     */
    val response = mock[Response].smart

    /**
     * The HTTP layer mock.
     */
    val httpClient = {
      val m = mock[HttpClient].smart
      m.execute(any[Request]()) returns Future.successful(response)
      m
    }

    /**
     * The Gravatar service config.
     */
    val config = spy(GravatarServiceConfig())

    /**
     * The Gravatar service implementation.
     */
    val service = new GravatarService(httpClient, config)

    /**
     * The email for which the Avatar should be retrieved.
     */
    val email = "apollonia.vanova@minutemen.com"

    /**
     * The generated hash for the email address.
     *
     * @see http://en.gravatar.com/site/check/
     */
    val hash = "0c91c2a94e7f613f82f08863c675ac5f"
  }
}
