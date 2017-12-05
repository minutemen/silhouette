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
package silhouette.http.transport

import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import silhouette.Credentials
import silhouette.crypto.Base64
import silhouette.http._

/**
 * Test case for the [[HeaderTransport]] class.
 */
class HeaderTransportSpec extends Specification {

  "The `copy` method" should {
    "allow to override the name" in new Context {
      transport.copy("not-name").name must be equalTo "not-name"
    }
  }

  "The `retrieve` method" should {
    "return some payload from the header with the given name" in new Context {
      transport.retrieve(requestPipeline) must beSome("payload")
    }

    "return None if no header with the give name exists" in new Context {
      override val request = FakeRequest()

      transport.retrieve(requestPipeline) must beNone
    }
  }

  "The `smuggle` method" should {
    "smuggle a header into the request" in new Context {
      override val request = FakeRequest()

      transport.smuggle("payload", requestPipeline).header("test").head must be equalTo "payload"
    }
  }

  "The `embed` method" should {
    "embed a header into the response" in new Context {
      override val response = FakeResponse()

      transport.embed("payload", responsePipeline).header("test").head must be equalTo "payload"
    }
  }

  "The `RetrieveFromHeader` reads" should {
    "return some payload from the header with the given name" in new Context {
      RetrieveFromHeader("test").read(requestPipeline) must beSome("payload")
    }

    "return None if no header with the give name exists" in new Context {
      RetrieveFromHeader("not-existing").read(requestPipeline) must beNone
    }
  }

  "The `RetrieveBearerTokenFromHeader` reads" should {
    "return some bearer token from the header with the given name" in new Context {
      override val request = FakeRequest(headers = Map("Authorization" -> Seq("Bearer token")))

      RetrieveBearerTokenFromHeader().read(requestPipeline) must beSome("token")
    }

    "return None if no header with the give name exists" in new Context {
      RetrieveBearerTokenFromHeader("not-existing").read(requestPipeline) must beNone
    }

    "return None if the header couldn't be extracted successfully" in new Context {
      override val request = FakeRequest(headers = Map("Authorization" -> Seq("wrong token")))

      RetrieveBearerTokenFromHeader().read(requestPipeline) must beNone
    }
  }

  "The `RetrieveBasicCredentialsFromHeader` reads" should {
    "return some basic credentials from the header with the given name" in new Context {
      override val request = FakeRequest(headers = Map("Authorization" -> Seq(s"Basic ${Base64.encode(s"user:pass")}")))

      RetrieveBasicCredentialsFromHeader().read(requestPipeline) must beSome(Credentials("user", "pass"))
    }

    "return None if no header with the give name exists" in new Context {
      RetrieveBasicCredentialsFromHeader("not-existing").read(requestPipeline) must beNone
    }

    "return None if the header couldn't be extracted successfully" in new Context {
      override val request = FakeRequest(headers = Map("Authorization" -> Seq("wrong: credentials")))

      RetrieveBasicCredentialsFromHeader().read(requestPipeline) must beNone
    }
  }

  "The `SmuggleIntoHeader` writes" should {
    "smuggle a header into the request" in new Context {
      SmuggleIntoHeader("test")
        .write(("payload", requestPipeline))
        .header("test").head must be equalTo "payload"
    }
  }

  "The `SmuggleBearerTokenIntoHeader` writes" should {
    "smuggle a bearer token header into the request" in new Context {
      SmuggleBearerTokenIntoHeader()
        .write(("token", requestPipeline))
        .header("Authorization").head must be equalTo "Bearer token"
    }
  }

  "The `SmuggleBasicCredentialsIntoHeader` writes" should {
    "smuggle a basic auth header into the request" in new Context {
      SmuggleBasicCredentialsIntoHeader()
        .write((Credentials("user", "pass"), requestPipeline))
        .header("Authorization").head must be equalTo s"Basic ${Base64.encode(s"user:pass")}"
    }
  }

  "The `EmbedIntoHeader` writes" should {
    "embed a header into the request" in new Context {
      EmbedIntoHeader("test")
        .write(("payload", responsePipeline))
        .header("test").head must be equalTo "payload"
    }
  }

  "The `SmuggleBearerTokenIntoHeader` writes" should {
    "embed a bearer token header into the request" in new Context {
      EmbedBearerTokenIntoHeader()
        .write(("token", responsePipeline))
        .header("Authorization").head must be equalTo "Bearer token"
    }
  }

  "The `EmbedBasicCredentialsIntoHeader` writes" should {
    "embed a basic auth header into the request" in new Context {
      EmbedBasicCredentialsIntoHeader()
        .write((Credentials("user", "pass"), responsePipeline))
        .header("Authorization").head must be equalTo s"Basic ${Base64.encode(s"user:pass")}"
    }
  }

  /**
   * The context.
   */
  trait Context extends Scope {

    /**
     * The header transport to test.
     */
    val transport = HeaderTransport("test")

    /**
     * A fake request.
     */
    val request = FakeRequest(headers = Map("test" -> Seq("payload")))

    /**
     * A fake response.
     */
    val response = FakeResponse()

    /**
     * A fake request pipeline.
     */
    lazy val requestPipeline = FakeRequestPipeline(request)

    /**
     * A fake response pipeline.
     */
    lazy val responsePipeline = FakeResponsePipeline(response)
  }
}
