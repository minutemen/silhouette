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
import silhouette.http._
import sttp.model.{ Header, HeaderNames }

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
      transport.retrieve(requestPipeline.withHeaders(Header("test", "payload"))) must
        beSome("payload")
    }

    "return None if no header with the give name exists" in new Context {
      transport.retrieve(requestPipeline) must beNone
    }
  }

  "The `smuggle` method" should {
    "smuggle a header into the request" in new Context {
      transport.smuggle("payload", requestPipeline).headerValue("test") must
        beSome("payload")
    }
  }

  "The `embed` method" should {
    "embed a header into the response" in new Context {
      transport.embed("payload", responsePipeline).headerValue("test") must
        beSome("payload")
    }
  }

  "The `RetrieveFromHeader` reads" should {
    "return some payload from the header with the given name" in new Context {
      RetrieveFromHeader("test")(
        requestPipeline.withHeaders(Header("test", "payload"))
      ) must beSome("payload")
    }

    "return None if no header with the give name exists" in new Context {
      RetrieveFromHeader("not-existing")(requestPipeline) must beNone
    }
  }

  "The `RetrieveBearerTokenFromHeader` reads" should {
    "return some bearer token from the header with the given name" in new Context {
      RetrieveBearerTokenFromHeader()(
        requestPipeline.withHeaders(BearerAuthorizationHeader(BearerToken("token")))
      ) must beSome(BearerToken("token"))
    }

    "return None if no header with the give name exists" in new Context {
      RetrieveBearerTokenFromHeader("not-existing")(requestPipeline) must beNone
    }

    "return None if the header couldn't be extracted successfully" in new Context {
      RetrieveBearerTokenFromHeader()(
        requestPipeline.withHeaders(Header.authorization("", "wrong token"))
      ) must beNone
    }
  }

  "The `RetrieveBasicCredentialsFromHeader` reads" should {
    "return some basic credentials from the header with the given name" in new Context {
      RetrieveBasicCredentialsFromHeader()(
        requestPipeline.withHeaders(BasicAuthorizationHeader(BasicCredentials("user", "pass")))
      ) must beSome(BasicCredentials("user", "pass"))
    }

    "return None if no header with the give name exists" in new Context {
      RetrieveBasicCredentialsFromHeader("not-existing")(requestPipeline) must beNone
    }

    "return None if the header couldn't be extracted successfully" in new Context {
      RetrieveBasicCredentialsFromHeader()(
        requestPipeline.withHeaders(Header.authorization("wrong", "credentials"))
      ) must beNone
    }
  }

  "The `SmuggleIntoHeader` writes" should {
    "smuggle a header into the request" in new Context {
      SmuggleIntoHeader("test")(requestPipeline)("payload").headerValue("test") must
        beSome("payload")
    }
  }

  "The `SmuggleBearerTokenIntoHeader` writes" should {
    "smuggle a bearer token header into the request" in new Context {
      SmuggleBearerTokenIntoHeader()(requestPipeline)(BearerToken("token"))
        .header(HeaderNames.Authorization) must beSome(BearerAuthorizationHeader(BearerToken("token")))
    }
  }

  "The `SmuggleBasicCredentialsIntoHeader` writes" should {
    "smuggle a basic auth header into the request" in new Context {
      SmuggleBasicCredentialsIntoHeader(HeaderNames.Authorization)(requestPipeline)(
        BasicCredentials("user", "pass")
      ).header(HeaderNames.Authorization) must
        beSome(BasicAuthorizationHeader(BasicCredentials("user", "pass")))
    }
  }

  "The `EmbedIntoHeader` writes" should {
    "embed a header into the request" in new Context {
      EmbedIntoHeader("test")(responsePipeline)("payload").header("test") must
        beSome(Header("test", "payload"))
    }
  }

  "The `SmuggleBearerTokenIntoHeader` writes" should {
    "embed a bearer token header into the request" in new Context {
      EmbedBearerTokenIntoHeader()(responsePipeline)(BearerToken("token"))
        .header(HeaderNames.Authorization) must beSome(BearerAuthorizationHeader(BearerToken("token")))
    }
  }

  "The `EmbedBasicCredentialsIntoHeader` writes" should {
    "embed a basic auth header into the request" in new Context {
      EmbedBasicCredentialsIntoHeader()(responsePipeline)(BasicCredentials("user", "pass"))
        .header(HeaderNames.Authorization) must beSome(BasicAuthorizationHeader(BasicCredentials("user", "pass")))
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
     * A request pipeline.
     */
    lazy val requestPipeline = Fake.request

    /**
     * A response pipeline.
     */
    lazy val responsePipeline = Fake.response
  }
}
