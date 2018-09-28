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
 * Test case for the [[SessionTransport]] class.
 */
class SessionTransportSpec extends Specification {

  "The `copy` method" should {
    "allow to override the key" in new Context {
      transport.copy("not-key").key must be equalTo "not-key"
    }
  }

  "The `retrieve` method" should {
    "return some payload from the session with the given key" in new Context {
      transport.retrieve(requestPipeline.withSession("test" -> "payload")) must beSome("payload")
    }

    "return None if no session with the give key exists" in new Context {
      transport.retrieve(requestPipeline) must beNone
    }
  }

  "The `smuggle` method" should {
    "smuggle a session value into the request" in new Context {
      transport.smuggle("payload", requestPipeline).session("test") must be equalTo "payload"
    }
  }

  "The `embed` method" should {
    "embed a session value into the response" in new Context {
      transport.embed("payload", responsePipeline).session("test") must be equalTo "payload"
    }
  }

  "The `discard` method" should {
    "remove the session for the given key" in new Context {
      transport.discard(responsePipeline).session must beEmpty
    }
  }

  "The `RetrieveFromSession` reads" should {
    "return some payload from the session with the given key" in new Context {
      RetrieveFromSession("test").read(
        requestPipeline.withSession("test" -> "payload")
      ) must beSome("payload")
    }

    "return None if no session with the give key exists" in new Context {
      RetrieveFromSession("not-existing").read(requestPipeline) must beNone
    }
  }

  "The `SmuggleIntoSession` writes" should {
    "smuggle a session value into the request" in new Context {
      SmuggleIntoSession("test")
        .write(("payload", requestPipeline))
        .session("test") must be equalTo "payload"
    }
  }

  "The `EmbedIntoSession` writes" should {
    "embed a session value into the response" in new Context {
      EmbedIntoSession("test")
        .write(("payload", responsePipeline))
        .session("test") must be equalTo "payload"
    }
  }

  "The `DiscardFromSession` writes" should {
    "remove the session for the given key" in new Context {
      DiscardFromSession("test").write(responsePipeline).session must beEmpty
    }
  }

  /**
   * The context.
   */
  trait Context extends Scope {

    /**
     * The session transport to test.
     */
    val transport = SessionTransport("test")

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
