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

/**
 * Test case for the [[CookieTransport]] class.
 */
class CookieTransportSpec extends Specification {

  "The `copy` method" should {
    "allow to override the config" in new Context {
      transport.copy(transport.config.copy("not-name")).config.name must be equalTo "not-name"
    }
  }

  "The `retrieve` method" should {
    "return some payload from the cookie with the given name" in new Context {
      transport.retrieve(requestPipeline.withCookies(Cookie("test", "payload"))) must beSome("payload")
    }

    "return None if no cookie with the give name exists" in new Context {
      transport.retrieve(requestPipeline) must beNone
    }
  }

  "The `smuggle` method" should {
    "smuggle a cookie into the request" in new Context {
      transport.smuggle("payload", requestPipeline).cookie("test") must beSome[Cookie].like {
        case cookie =>
          cookie.value must be equalTo "payload"
      }
    }
  }

  "The `embed` method" should {
    "embed a cookie into the response" in new Context {
      transport.embed("payload", responsePipeline).cookie("test") must beSome[Cookie].like {
        case cookie =>
          cookie.value must be equalTo "payload"
      }
    }
  }

  "The `discard` method" should {
    "discard a cookie" in new Context {
      transport.discard(responsePipeline).cookie("test") must beSome[Cookie].like {
        case cookie =>
          cookie.value must be equalTo ""
          cookie.maxAge must beSome(-86400)
      }
    }
  }

  "The `RetrieveFromCookie` reads" should {
    "read some payload from a cookie stored in the request" in new Context {
      RetrieveFromCookie("test")(
        requestPipeline.withCookies(Cookie("test", "payload"))
      ) must beSome("payload")
    }

    "return None if no cookie with the give name exists" in new Context {
      RetrieveFromCookie("noz-existing")(requestPipeline) must beNone
    }
  }

  "The `SmuggleIntoCookie` writes" should {
    "smuggle a cookie into the request" in new Context {
      SmuggleIntoCookie(config)(requestPipeline)("payload")
        .cookie("test") must beSome[Cookie].like {
          case cookie =>
            cookie.value must be equalTo "payload"
        }
    }
  }

  "The `EmbedIntoCookie` writes" should {
    "embed a cookie into the response" in new Context {
      EmbedIntoCookie(config)(responsePipeline)("payload")
        .cookie("test") must beSome[Cookie].like {
          case cookie =>
            cookie.value must be equalTo "payload"
        }
    }
  }

  "The `DiscardCookie` writes" should {
    "discard a cookie" in new Context {
      DiscardCookie(config)(responsePipeline)
        .cookie("test") must beSome[Cookie].like {
          case cookie =>
            cookie.value must be equalTo ""
            cookie.maxAge must beSome(-86400)
        }
    }
  }

  /**
   * The context.
   */
  trait Context extends Scope {

    /**
     * The cookie transport config.
     */
    val config = CookieTransportConfig(name = "test")

    /**
     * The cookie transport to test.
     */
    val transport = CookieTransport(config)

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
