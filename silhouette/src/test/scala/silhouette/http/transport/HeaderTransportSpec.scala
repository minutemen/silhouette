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
 * Test case for the [[HeaderTransport]] class.
 */
class HeaderTransportSpec extends Specification {

  "The `withSettings` method" should {
    "allow to override the settings" in new Context {
      transport.withSettings(_.copy("not-name")).settings.name must be equalTo "not-name"
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

  "The `embed` method" should {
    "embed a header into the request" in new Context {
      override val request = FakeRequest()

      transport.embed("payload", requestPipeline).header("test").head must be equalTo "payload"
    }
  }

  "The `embed` method" should {
    "embed a header into the response" in new Context {
      override val response = FakeResponse()

      transport.embed("payload", responsePipeline).header("test").head must be equalTo "payload"
    }
  }

  "The `discard` method" should {
    "return the unchanged response" in new Context {
      override val response = FakeResponse()

      transport.discard(responsePipeline) should be equalTo responsePipeline
    }
  }

  /**
   * The context.
   */
  trait Context extends Scope {

    /**
     * The header transport settings.
     */
    val settings = HeaderTransportSettings(name = "test")

    /**
     * The header transport to test.
     */
    val transport = HeaderTransport(settings)

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
