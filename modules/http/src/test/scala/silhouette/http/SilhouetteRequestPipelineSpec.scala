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

import java.net.URI

import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import silhouette.crypto.Hash
import silhouette.crypto.Hash._

/**
 * Test case for the [[SilhouetteRequestPipeline]] class.
 */
class SilhouetteRequestPipelineSpec extends Specification {

  "The `headers` method" should {
    "return all headers" in new Context {
      requestPipeline.headers must be equalTo request.headers
    }
  }

  "The `header` method" should {
    "return the list of header values" in new Context {
      requestPipeline.header("TEST1") must beSome(Header("TEST1", "value1", "value2"))
    }

    "return an empty list if no header with the given name was found" in new Context {
      requestPipeline.header("TEST3") must beNone
    }
  }

  "The `withHeaders` method" should {
    "append a new header" in new Context {
      requestPipeline.withHeaders(Header("TEST3", "value1")).headers must be equalTo Seq(
        Header("TEST1", "value1", "value2"),
        Header("TEST2", "value1"),
        Header("TEST3", "value1")
      )
    }

    "append multiple headers" in new Context {
      requestPipeline.withHeaders(Header("TEST3", "value1"), Header("TEST4", "value1")).headers must be equalTo Seq(
        Header("TEST1", "value1", "value2"),
        Header("TEST2", "value1"),
        Header("TEST3", "value1"),
        Header("TEST4", "value1")
      )
    }

    "append multiple headers with the same name" in new Context {
      requestPipeline.withHeaders(Header("TEST3", "value1"), Header("TEST3", "value2", "value3")).headers must
        be equalTo Seq(
          Header("TEST1", "value1", "value2"),
          Header("TEST2", "value1"),
          Header("TEST3", "value1", "value2", "value3")
        )
    }

    "override an existing header" in new Context {
      requestPipeline.withHeaders(Header("TEST2", "value2"), Header("TEST2", "value3")).headers must be equalTo Seq(
        Header("TEST1", "value1", "value2"),
        Header("TEST2", "value2", "value3")
      )
    }

    "override multiple existing headers" in new Context {
      requestPipeline.withHeaders(Header("TEST1", "value3"), Header("TEST2", "value2")).headers must be equalTo Seq(
        Header("TEST1", "value3"),
        Header("TEST2", "value2")
      )
    }
  }

  "The `cookies` method" should {
    "return all cookies" in new Context {
      requestPipeline.cookies must be equalTo request.cookies
    }
  }

  "The `cookie` method" should {
    "return some cookie for the given name" in new Context {
      requestPipeline.cookie("test1") must beSome(Cookie("test1", "value1"))
    }

    "return None if no cookie with the given name was found" in new Context {
      requestPipeline.cookie("test3") must beNone
    }
  }

  "The `withCookies` method" should {
    "append a new cookie" in new Context {
      requestPipeline.withCookies(Cookie("test3", "value3")).cookies must be equalTo Seq(
        Cookie("test1", "value1"),
        Cookie("test2", "value2"),
        Cookie("test3", "value3")
      )
    }

    "override an existing cookie" in new Context {
      requestPipeline.withCookies(Cookie("test1", "value3")).cookies must be equalTo Seq(
        Cookie("test1", "value3"),
        Cookie("test2", "value2")
      )
    }

    "use the last cookie if multiple cookies with the same name are given" in new Context {
      requestPipeline.withCookies(Cookie("test1", "value3"), Cookie("test1", "value4")).cookies must be equalTo Seq(
        Cookie("test1", "value4"),
        Cookie("test2", "value2")
      )
    }
  }

  "The `rawQueryString` method" should {
    "return the raw query string" in new Context {
      requestPipeline.rawQueryString must be equalTo "test1=value1&test1=value2&test2=value1"
    }

    "be URL encoded" in new Context {
      requestPipeline.withQueryParams("test=3" -> "value=4").rawQueryString must be equalTo
        "test1=value1&test1=value2&test2=value1&test%3D3=value%3D4"
    }
  }

  "The `queryParams` method" should {
    "return all query params" in new Context {
      requestPipeline.queryParams must be equalTo request.queryParams
    }
  }

  "The `queryParam` method" should {
    "return the list of query params" in new Context {
      requestPipeline.queryParam("test1") must be equalTo Seq("value1", "value2")
    }

    "return an empty list if no query param with the given name was found" in new Context {
      requestPipeline.queryParam("test3") must beEmpty
    }
  }

  "The `withQueryParams` method" should {
    "append a new query param" in new Context {
      requestPipeline.withQueryParams("test3" -> "value1").queryParams must be equalTo Map(
        "test1" -> Seq("value1", "value2"),
        "test2" -> Seq("value1"),
        "test3" -> Seq("value1")
      )
    }

    "append multiple query params" in new Context {
      requestPipeline.withQueryParams("test3" -> "value1", "test4" -> "value1").queryParams must be equalTo Map(
        "test1" -> Seq("value1", "value2"),
        "test2" -> Seq("value1"),
        "test3" -> Seq("value1"),
        "test4" -> Seq("value1")
      )
    }

    "append multiple query params with the same name" in new Context {
      requestPipeline.withQueryParams("test3" -> "value1", "test3" -> "value2").queryParams must be equalTo Map(
        "test1" -> Seq("value1", "value2"),
        "test2" -> Seq("value1"),
        "test3" -> Seq("value1", "value2")
      )
    }

    "override an existing query param" in new Context {
      requestPipeline.withQueryParams("test2" -> "value2", "test2" -> "value3").queryParams must be equalTo Map(
        "test1" -> Seq("value1", "value2"),
        "test2" -> Seq("value2", "value3")
      )
    }

    "override multiple existing query params" in new Context {
      requestPipeline.withQueryParams("test1" -> "value3", "test2" -> "value2").queryParams must be equalTo Map(
        "test1" -> Seq("value3"),
        "test2" -> Seq("value2")
      )
    }
  }

  "The default `fingerprint` method" should {
    "return fingerprint including the `User-Agent` header" in new Context {
      val userAgent = "test-user-agent"
      requestPipeline.withHeaders(Header("User-Agent", userAgent)).fingerprint must
        be equalTo Hash.sha1(userAgent + "::")
    }

    "return fingerprint including the `Accept-Language` header" in new Context {
      val acceptLanguage = "test-accept-language"
      requestPipeline.withHeaders(Header("Accept-Language", acceptLanguage)).fingerprint must
        be equalTo Hash.sha1(":" + acceptLanguage + ":")
    }

    "return fingerprint including the `Accept-Charset` header" in new Context {
      val acceptCharset = "test-accept-charset"
      requestPipeline.withHeaders(Header("Accept-Charset", acceptCharset)).fingerprint must
        be equalTo Hash.sha1("::" + acceptCharset)
    }

    "return fingerprint including all values" in new Context {
      val userAgent = "test-user-agent"
      val acceptLanguage = "test-accept-language"
      val acceptCharset = "test-accept-charset"
      requestPipeline.withHeaders(
        Header("User-Agent", userAgent),
        Header("Accept-Language", acceptLanguage),
        Header("Accept-Charset", acceptCharset)
      ).fingerprint must be equalTo Hash.sha1(
          userAgent + ":" + acceptLanguage + ":" + acceptCharset
        )
    }
  }

  "The custom `fingerprint` method" should {
    "return a fingerprint created by a generator function" in new Context {
      val userAgent = "test-user-agent"
      val acceptLanguage = "test-accept-language"
      val acceptCharset = "test-accept-charset"
      requestPipeline.withHeaders(
        Header(Header.Name.`User-Agent`, userAgent),
        Header(Header.Name.`Accept-Language`, acceptLanguage),
        Header(Header.Name.`Accept-Charset`, acceptCharset),
        Header(Header.Name.`Accept-Encoding`, "gzip", "deflate")
      ).fingerprint(request => Hash.sha1(new StringBuilder()
          .append(request.headers.find(_.name == Header.Name.`User-Agent`).map(_.value).getOrElse("")).append(":")
          .append(request.headers.find(_.name == Header.Name.`Accept-Language`).map(_.value).getOrElse("")).append(":")
          .append(request.headers.find(_.name == Header.Name.`Accept-Charset`).map(_.value).getOrElse("")).append(":")
          .append(request.headers.find(_.name == Header.Name.`Accept-Encoding`).map(_.value).getOrElse(""))
          .toString()
        )) must be equalTo Hash.sha1(
          userAgent + ":" + acceptLanguage + ":" + acceptCharset + ":gzip,deflate"
        )
    }
  }

  "The `unbox` method" should {
    "return the handled request" in new Context {
      requestPipeline.unbox must be equalTo request
    }
  }

  /**
   * The context.
   */
  trait Context extends Scope {

    /**
     * A request.
     */
    val request = SilhouetteRequest(
      uri = new URI("http://localhost"),
      method = Method.POST,
      headers = Seq(
        Header("TEST1", "value1", "value2"),
        Header("TEST2", "value1")
      ),
      cookies = Seq(
        Cookie("test1", "value1"),
        Cookie("test2", "value2")
      ),
      queryParams = Map(
        "test1" -> Seq("value1", "value2"),
        "test2" -> Seq("value1")
      )
    )

    /**
     * A request pipeline which handles a request.
     */
    val requestPipeline = SilhouetteRequestPipeline(request)
  }
}
