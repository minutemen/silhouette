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
package silhouette.http

import org.specs2.mutable.Specification
import org.specs2.specification.Scope

/**
 * Test case for the [[SilhouetteResponsePipeline]] class.
 */
class SilhouetteResponsePipelineSpec extends Specification {

  "The `headers` method" should {
    "return all headers" in new Context {
      responsePipeline.headers must be equalTo response.headers
    }
  }

  "The `header` method" should {
    "return the list of header values" in new Context {
      responsePipeline.header("TEST1") must beSome(Header("TEST1", "value1", "value2"))
    }

    "return an empty list if no header with the given name was found" in new Context {
      responsePipeline.header("TEST3") must beNone
    }
  }

  "The `withHeaders` method" should {
    "append a new header" in new Context {
      responsePipeline.withHeaders(Header("TEST3", "value1")).headers must be equalTo Seq(
        Header("TEST1", "value1", "value2"),
        Header("TEST2", "value1"),
        Header("TEST3", "value1")
      )
    }

    "append multiple headers" in new Context {
      responsePipeline.withHeaders(Header("TEST3", "value1"), Header("TEST4", "value1")).headers must be equalTo Seq(
        Header("TEST1", "value1", "value2"),
        Header("TEST2", "value1"),
        Header("TEST3", "value1"),
        Header("TEST4", "value1")
      )
    }

    "append multiple headers with the same name" in new Context {
      responsePipeline.withHeaders(Header("TEST3", "value1"), Header("TEST3", "value2", "value3")).headers must
        be equalTo Seq(
          Header("TEST1", "value1", "value2"),
          Header("TEST2", "value1"),
          Header("TEST3", "value1", "value2", "value3")
        )
    }

    "override an existing header" in new Context {
      responsePipeline.withHeaders(Header("TEST2", "value2"), Header("TEST2", "value3")).headers must be equalTo Seq(
        Header("TEST1", "value1", "value2"),
        Header("TEST2", "value2", "value3")
      )
    }

    "override multiple existing headers" in new Context {
      responsePipeline.withHeaders(Header("TEST1", "value3"), Header("TEST2", "value2")).headers must be equalTo Seq(
        Header("TEST1", "value3"),
        Header("TEST2", "value2")
      )
    }
  }

  "The `cookies` method" should {
    "return all cookies" in new Context {
      responsePipeline.cookies must be equalTo response.cookies
    }
  }

  "The `cookie` method" should {
    "return some cookie for the given name" in new Context {
      responsePipeline.cookie("test1") must beSome(Cookie("test1", "value1"))
    }

    "return None if no cookie with the given name was found" in new Context {
      responsePipeline.cookie("test3") must beNone
    }
  }

  "The `withCookies` method" should {
    "append a new cookie" in new Context {
      responsePipeline.withCookies(Cookie("test3", "value3")).cookies must be equalTo Seq(
        Cookie("test1", "value1"),
        Cookie("test2", "value2"),
        Cookie("test3", "value3")
      )
    }

    "override an existing cookie" in new Context {
      responsePipeline.withCookies(Cookie("test1", "value3")).cookies must be equalTo Seq(
        Cookie("test1", "value3"),
        Cookie("test2", "value2")
      )
    }

    "use the last cookie if multiple cookies with the same name are given" in new Context {
      responsePipeline.withCookies(Cookie("test1", "value3"), Cookie("test1", "value4")).cookies must be equalTo Seq(
        Cookie("test1", "value4"),
        Cookie("test2", "value2")
      )
    }
  }

  "The `unbox` method" should {
    "return the handled response" in new Context {
      responsePipeline.unbox must be equalTo response
    }
  }

  /**
   * The context.
   */
  trait Context extends Scope {

    /**
     * A response.
     */
    val response = SilhouetteResponse(
      status = Status.OK,
      headers = Seq(
        Header("TEST1", "value1", "value2"),
        Header("TEST2", "value1")
      ),
      cookies = Seq(
        Cookie("test1", "value1"),
        Cookie("test2", "value2")
      )
    )

    /**
     * A response pipeline which handles a response.
     */
    val responsePipeline = SilhouetteResponsePipeline(response)
  }
}
