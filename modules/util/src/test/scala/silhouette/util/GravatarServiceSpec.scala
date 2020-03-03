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

import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import silhouette.util.GravatarService._
import sttp.client.Identity
import sttp.client.testing.SttpBackendStub

/**
 * Test case for the [[GravatarService]] class.
 */
class GravatarServiceSpec extends Specification with Mockito {

  "The `retrieveURI` method" should {
    "return None if email is empty" in new Context {
      service.retrieveURI("") should beNone
    }

    "return None if HTTP status code isn't 200" in new Context {
      override implicit val sttpBackend: SttpBackendStub[Identity, Nothing] =
        SttpBackendStub.synchronous.whenAnyRequest.thenRespondNotFound

      service.retrieveURI(email) should beNone
    }

    "return secure Avatar URI" in new Context {
      override implicit val sttpBackend: SttpBackendStub[Identity, Nothing] =
        SttpBackendStub.synchronous.whenAnyRequest.thenRespondOk()

      service.retrieveURI(email) should beSome(SecureURI.format(hash, "?d=404").toJavaURI)
    }

    "return insecure Avatar URI" in new Context {
      config.secure returns false
      override implicit val sttpBackend: SttpBackendStub[Identity, Nothing] =
        SttpBackendStub.synchronous.whenAnyRequest.thenRespondOk()

      service.retrieveURI(email) should beSome(InsecureURI.format(hash, "?d=404").toJavaURI)
    }

    "return an URI with additional parameters" in new Context {
      config.params returns Map("d" -> "http://example.com/images/avatar.jpg", "s" -> "400")
      override implicit val sttpBackend: SttpBackendStub[Identity, Nothing] =
        SttpBackendStub.synchronous.whenAnyRequest.thenRespondOk()

      service.retrieveURI(email) should beSome(
        SecureURI.format(hash, "?d=http%3A%2F%2Fexample.com%2Fimages%2Favatar.jpg&s=400").toJavaURI
      )
    }

    "not trim leading zeros" in new Context {
      override implicit val sttpBackend: SttpBackendStub[Identity, Nothing] =
        SttpBackendStub.synchronous.whenAnyRequest.thenRespondOk()

      service.retrieveURI("123test@test.com") should beSome(
        SecureURI.format("0d77aed6b4c5857473c9a04c2017f8b8", "?d=404").toJavaURI
      )
    }
  }

  /**
   * The context.
   */
  trait Context extends Scope {

    /**
     * The STTP backend.
     */
    implicit val sttpBackend: SttpBackendStub[Identity, Nothing] = SttpBackendStub.synchronous

    /**
     * The Gravatar service config.
     */
    val config = spy(GravatarServiceConfig())

    /**
     * The Gravatar service implementation.
     */
    lazy val service = GravatarService(config)

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
