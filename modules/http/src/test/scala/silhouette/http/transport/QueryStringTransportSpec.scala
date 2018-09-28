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
import silhouette.http.Fake

/**
 * Test case for the [[QueryStringTransport]] class.
 */
class QueryStringTransportSpec extends Specification {

  "The `copy` method" should {
    "allow to override the name" in new Context {
      transport.copy("not-name").name must be equalTo "not-name"
    }
  }

  "The `retrieve` method" should {
    "return some payload from the query param with the given name" in new Context {
      transport.retrieve(
        requestPipeline.withQueryParams("test" -> "payload")
      ) must beSome("payload")
    }

    "return None if no query param with the give name exists" in new Context {
      transport.retrieve(requestPipeline) must beNone
    }
  }

  "The `smuggle` method" should {
    "smuggle a query param into the request" in new Context {
      transport.smuggle("payload", requestPipeline)
        .queryParam("test").head must be equalTo "payload"
    }
  }

  "The `RetrieveFromQueryString` reads" should {
    "return some payload from the query param with the given name" in new Context {
      RetrieveFromQueryString("test").read(
        requestPipeline.withQueryParams("test" -> "payload")
      ) must beSome("payload")
    }

    "return None if no query param with the give name exists" in new Context {
      RetrieveFromQueryString("not-existing").read(requestPipeline) must beNone
    }
  }

  "The `SmuggleIntoQueryString` writes" should {
    "smuggle a query param into the request" in new Context {
      SmuggleIntoQueryString("test")
        .write(("payload", requestPipeline))
        .queryParam("test").head must be equalTo "payload"
    }
  }

  /**
   * The context.
   */
  trait Context extends Scope {

    /**
     * The query string transport to test.
     */
    val transport = QueryStringTransport("test")

    /**
     * A request pipeline.
     */
    lazy val requestPipeline = Fake.request
  }
}
