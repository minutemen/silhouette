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

  "The `withSettings` method" should {
    "allow to override the settings" in new Context {
      transport.withSettings(_.copy("not-name")).settings.key must be equalTo "not-name"
    }
  }

  "The `retrieve` method" should {
    "return some payload from the session with the given key" in new Context {
      transport.retrieve(requestPipeline) must beSome("payload")
    }

    "return None if no session with the give key exists" in new Context {
      override val request = FakeRequest()

      transport.retrieve(requestPipeline) must beNone
    }
  }

  "The `embed` method" should {
    "embed a session value into the request" in new Context {
      override val request = FakeRequest()

      transport.embed("payload", requestPipeline).session("test") must be equalTo "payload"
    }
  }

  "The `embed` method" should {
    "embed a session value into the response" in new Context {
      override val response = FakeResponse()

      transport.embed("payload", responsePipeline).session("test") must be equalTo "payload"
    }
  }

  "The `discard` method" should {
    "remove the session for the given key" in new Context {
      override val response = FakeResponse()

      transport.discard(responsePipeline).session must beEmpty
    }
  }

  /**
   * The context.
   */
  trait Context extends Scope {

    /**
     * The session transport settings.
     */
    val settings = SessionTransportSettings(key = "test")

    /**
     * The session transport to test.
     */
    val transport = SessionTransport[String](settings)

    /**
     * A fake request.
     */
    val request = FakeRequest(session = Map("test" -> "payload"))

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
