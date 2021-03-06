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

import io.circe.Json
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import silhouette.crypto.Hash
import silhouette.crypto.Hash._
import silhouette.http.Body._
import silhouette.http.BodyWriter._
import sttp.model.Uri._
import sttp.model._

import scala.collection.immutable.Seq
import scala.xml.{ Node, XML }

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
      requestPipeline.header("TEST1") must beSome(Header("TEST1", "value1, value2"))
    }

    "return an empty list if no header with the given name was found" in new Context {
      requestPipeline.header("TEST3") must beNone
    }
  }

  "The `withHeaders` method" should {
    "append a new header" in new Context {
      requestPipeline
        .withHeaders(
          Header("TEST3", "value1")
        )
        .headers must be equalTo Headers(
        Seq(
          Header("TEST1", "value1, value2"),
          Header("TEST2", "value1"),
          Header("TEST3", "value1")
        )
      )
    }

    "append multiple headers" in new Context {
      requestPipeline
        .withHeaders(
          Header("TEST3", "value1"),
          Header("TEST4", "value1")
        )
        .headers must be equalTo Headers(
        Seq(
          Header("TEST1", "value1, value2"),
          Header("TEST2", "value1"),
          Header("TEST3", "value1"),
          Header("TEST4", "value1")
        )
      )
    }

    "append multiple headers with the same name" in new Context {
      requestPipeline
        .withHeaders(
          Header("TEST3", "value1"),
          Header("TEST3", "value2, value3")
        )
        .headers must be equalTo Headers(
        Seq(
          Header("TEST1", "value1, value2"),
          Header("TEST2", "value1"),
          Header("TEST3", "value1"),
          Header("TEST3", "value2, value3")
        )
      )
    }

    "not override any existing header" in new Context {
      requestPipeline
        .withHeaders(
          Header("TEST1", "value1, value2"),
          Header("TEST2", "value2"),
          Header("TEST2", "value3")
        )
        .headers must be equalTo Headers(
        Seq(
          Header("TEST1", "value1, value2"),
          Header("TEST2", "value1"),
          Header("TEST1", "value1, value2"),
          Header("TEST2", "value2"),
          Header("TEST2", "value3")
        )
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
      requestPipeline.cookie("test1") must beSome(CookieWithMeta.unsafeApply("test1", "value1"))
    }

    "return None if no cookie with the given name was found" in new Context {
      requestPipeline.cookie("test3") must beNone
    }
  }

  "The `withCookies` method" should {
    "append a new cookie" in new Context {
      requestPipeline.withCookies(CookieWithMeta.unsafeApply("test3", "value3")).cookies must be equalTo Seq(
        CookieWithMeta.unsafeApply("test1", "value1"),
        CookieWithMeta.unsafeApply("test2", "value2"),
        CookieWithMeta.unsafeApply("test3", "value3")
      )
    }

    "override an existing cookie" in new Context {
      requestPipeline.withCookies(CookieWithMeta.unsafeApply("test1", "value3")).cookies must be equalTo Seq(
        CookieWithMeta.unsafeApply("test1", "value3"),
        CookieWithMeta.unsafeApply("test2", "value2")
      )
    }

    "use the last cookie if multiple cookies with the same name are given" in new Context {
      requestPipeline
        .withCookies(
          CookieWithMeta.unsafeApply("test1", "value3"),
          CookieWithMeta.unsafeApply("test1", "value4")
        )
        .cookies must be equalTo Seq(
        CookieWithMeta.unsafeApply("test1", "value4"),
        CookieWithMeta.unsafeApply("test2", "value2")
      )
    }
  }

  "The `rawQueryString` method" should {
    "return the raw query string" in new Context {
      requestPipeline.rawQueryString must be equalTo "test1=value1&test1=value2&test2=value2"
    }

    "be URL encoded" in new Context {
      requestPipeline.withQueryParams("test=3" -> "value=4").rawQueryString must be equalTo
        "test1=value1&test1=value2&test2=value2&test%3D3=value%3D4"
    }
  }

  "The `queryParams` method" should {
    "return all query params" in new Context {
      requestPipeline.queryParams.toMap must be equalTo request.uri.params.toMap
    }
  }

  "The `withQueryParams` method" should {
    "append a new query param" in new Context {
      requestPipeline
        .withQueryParams(
          "test3" -> "value1"
        )
        .queryParams
        .toMap must be equalTo QueryParams
        .fromMultiMap(
          Map(
            "test1" -> Seq("value1", "value2"),
            "test2" -> Seq("value2"),
            "test3" -> Seq("value1")
          )
        )
        .toMap
    }

    "append multiple query params" in new Context {
      requestPipeline
        .withQueryParams(
          "test3" -> "value3",
          "test4" -> "value4"
        )
        .queryParams
        .toMap must be equalTo QueryParams
        .fromMultiMap(
          Map(
            "test1" -> Seq("value1", "value2"),
            "test2" -> Seq("value2"),
            "test3" -> Seq("value3"),
            "test4" -> Seq("value4")
          )
        )
        .toMap
    }

    "append multiple query params with the same name" in new Context {
      requestPipeline
        .withQueryParams(
          "test3" -> "value1",
          "test3" -> "value2"
        )
        .queryParams
        .toMap must be equalTo QueryParams
        .fromMultiMap(
          Map(
            "test1" -> Seq("value1", "value2"),
            "test2" -> Seq("value2"),
            "test3" -> Seq("value1", "value2")
          )
        )
        .toMap
    }

    "override an existing query param" in new Context {
      requestPipeline
        .withQueryParams(
          "test2" -> "value2",
          "test2" -> "value3"
        )
        .queryParams
        .toMap must be equalTo QueryParams
        .fromMultiMap(
          Map(
            "test1" -> Seq("value1", "value2"),
            "test2" -> Seq("value2", "value3")
          )
        )
        .toMap
    }

    "override multiple existing query params" in new Context {
      requestPipeline
        .withQueryParams(
          "test1" -> "value3",
          "test2" -> "value2"
        )
        .queryParams
        .toMap must be equalTo QueryParams
        .fromMultiMap(
          Map(
            "test1" -> Seq("value3"),
            "test2" -> Seq("value2")
          )
        )
        .toMap
    }
  }

  "The `withBodyExtractor` method" should {
    "set a new body extractor for the request" in new Context {
      requestPipeline
        .withBodyExtractor(new CustomBodyExtractor)
        .bodyExtractor
        .raw(request.body) must be equalTo "custom"
    }
  }

  "The default `fingerprint` method" should {
    "return fingerprint including the `User-Agent` header" in new Context {
      val userAgent = "test-user-agent"
      requestPipeline.withHeaders(Header("User-Agent", userAgent)).fingerprint() must
        be equalTo Hash.sha1(userAgent + "::")
    }

    "return fingerprint including the `Accept-Language` header" in new Context {
      val acceptLanguage = "test-accept-language"
      requestPipeline.withHeaders(Header("Accept-Language", acceptLanguage)).fingerprint() must
        be equalTo Hash.sha1(":" + acceptLanguage + ":")
    }

    "return fingerprint including the `Accept-Charset` header" in new Context {
      val acceptCharset = "test-accept-charset"
      requestPipeline.withHeaders(Header("Accept-Charset", acceptCharset)).fingerprint() must
        be equalTo Hash.sha1("::" + acceptCharset)
    }

    "return fingerprint including all values" in new Context {
      val userAgent = "test-user-agent"
      val acceptLanguage = "test-accept-language"
      val acceptCharset = "test-accept-charset"
      requestPipeline
        .withHeaders(
          Header("User-Agent", userAgent),
          Header("Accept-Language", acceptLanguage),
          Header("Accept-Charset", acceptCharset)
        )
        .fingerprint() must be equalTo Hash.sha1(
        userAgent + ":" + acceptLanguage + ":" + acceptCharset
      )
    }
  }

  "The custom `fingerprint` method" should {
    "return a fingerprint created by a generator function" in new Context {
      val userAgent = "test-user-agent"
      val acceptLanguage = "test-accept-language"
      val acceptCharset = "test-accept-charset"
      val acceptEncoding = "gzip, deflate"
      requestPipeline
        .withHeaders(
          Header(HeaderNames.UserAgent, userAgent),
          Header(HeaderNames.AcceptLanguage, acceptLanguage),
          Header(HeaderNames.AcceptCharset, acceptCharset),
          Header(HeaderNames.AcceptEncoding, acceptEncoding)
        )
        .fingerprint(request =>
          Hash.sha1(
            new StringBuilder()
              .append(request.headerValue(HeaderNames.UserAgent).getOrElse(""))
              .append(":")
              .append(request.headerValue(HeaderNames.AcceptLanguage).getOrElse(""))
              .append(":")
              .append(request.headerValue(HeaderNames.AcceptCharset).getOrElse(""))
              .append(":")
              .append(request.headerValue(HeaderNames.AcceptEncoding).getOrElse(""))
              .toString()
          )
        ) must be equalTo Hash.sha1(
        userAgent + ":" + acceptLanguage + ":" + acceptCharset + ":" + acceptEncoding
      )
    }
  }

  "The `unbox` method" should {
    "return the handled request" in new Context {
      requestPipeline.unbox must be equalTo request
    }
  }

  "The `extractString` method" should {
    "extract a value from query string if all parts are allowed" in new Context {
      requestPipeline.extractString("test1") must beSome("value1")
    }

    "extract a value from query string if part is allowed" in new Context {
      requestPipeline.extractString("test1", Some(Seq(RequestPart.QueryString))) must beSome("value1")
    }

    "do not extract a value from query string if part isn't allowed" in new Context {
      requestPipeline.extractString("test1", Some(Seq())) must beNone
    }

    "extract a value from headers if all parts are allowed" in new Context {
      requestPipeline.extractString("TEST1") must beSome("value1, value2")
    }

    "extract a value from headers if part is allowed" in new Context {
      requestPipeline.extractString("TEST1", Some(Seq(RequestPart.Headers))) must beSome("value1, value2")
    }

    "do not extract a value from headers if part isn't allowed" in new Context {
      requestPipeline.extractString("TEST1", Some(Seq())) must beNone
    }

    "extract a value from URL encoded body if all parts are allowed" in new Context {
      override val requestPipeline = withBody(Body.from(Map("code" -> Seq("value"))))

      requestPipeline.extractString("code") must beSome("value")
    }

    "extract a value from URL encoded body if part is allowed" in new Context {
      override val requestPipeline = withBody(Body.from(Map("code" -> Seq("value"))))

      requestPipeline.extractString("code", Some(Seq(RequestPart.FormUrlEncodedBody))) must beSome("value")
    }

    "do not extract a value from URL encoded body if part isn't allowed" in new Context {
      override val requestPipeline = withBody(Body.from(Map("code" -> Seq("value"))))

      requestPipeline.extractString("code", Some(Seq())) must beNone
    }

    "return None if the value couldn't be found in form URL encoded body" in new Context {
      override val requestPipeline = withBody(Body.from(Map("code" -> Seq("value"))))

      requestPipeline.extractString("none") must beNone
    }

    "return None if an error occurred during the extraction from a forum URL encoded body" in new Context {
      override val requestPipeline = withBody(
        Body(FormUrlEncodedBody.contentType, Body.DefaultCodec, "%".getBytes(Body.DefaultCodec.charSet))
      )

      requestPipeline.extractString("none") must beNone
    }

    "extract a value from Json body if all parts are allowed" in new Context {
      override val requestPipeline = withBody(Body.from(Json.obj("code" -> Json.fromString("value"))))

      requestPipeline.extractString("code") must beSome("value")
    }

    "extract a value from Json body if part is allowed" in new Context {
      override val requestPipeline = withBody(Body.from(Json.obj("code" -> Json.fromString("value"))))

      requestPipeline.extractString("code", Some(Seq(RequestPart.JsonBody))) must beSome("value")
    }

    "do not extract a value from Json body if part isn't allowed" in new Context {
      override val requestPipeline = withBody(Body.from(Json.obj("code" -> Json.fromString("value"))))

      requestPipeline.extractString("code", Some(Seq())) must beNone
    }

    "return None if the value couldn't be found in JSON body" in new Context {
      override val requestPipeline = withBody(Body.from(Json.obj("code" -> Json.fromString("value"))))

      requestPipeline.extractString("none") must beNone
    }

    "return None if an error occurred during the extraction from a JSON body" in new Context {
      override val requestPipeline = withBody(
        Body(JsonBody.contentType, Body.DefaultCodec, "{".getBytes(Body.DefaultCodec.charSet))
      )

      requestPipeline.extractString("none") must beNone
    }

    "extract a value from XML body if all parts are allowed" in new Context {
      override val requestPipeline = withBody(Body.from(XML.loadString("<code>value</code>"): Node))

      requestPipeline.extractString("code") must beSome("value")
    }

    "extract a value from XML body if part is allowed" in new Context {
      override val requestPipeline = withBody(Body.from(XML.loadString("<code>value</code>"): Node))

      requestPipeline.extractString("code", Some(Seq(RequestPart.XMLBody))) must beSome("value")
    }

    "do not extract a value from XML body if part isn't allowed" in new Context {
      override val requestPipeline = withBody(Body.from(XML.loadString("<code>value</code>"): Node))

      requestPipeline.extractString("code", Some(Seq())) must beNone
    }

    "return None if the value couldn't be found in XML body" in new Context {
      override val requestPipeline = withBody(Body.from(XML.loadString("<code>value</code>"): Node))

      requestPipeline.extractString("none") must beNone
    }

    "return None if an error occurred during the extraction from a XML body" in new Context {
      override val requestPipeline = withBody(
        Body(XmlBody.contentType, Body.DefaultCodec, "<code>".getBytes(Body.DefaultCodec.charSet))
      )

      requestPipeline.extractString("none") must beNone
    }

    "return None if no value could be found in the request" in new Context {
      requestPipeline.extractString("none") must beNone
    }
  }

  /**
   * The context.
   */
  trait Context extends Scope {

    /**
     * A custom body extractor for testing.
     */
    case class CustomBodyExtractor() extends SilhouetteRequestBodyExtractor {
      override def raw(body: Option[Body]): String = "custom"
    }

    /**
     * A request.
     */
    val request = SilhouetteRequest(
      uri = uri"http://localhost?test1=value1&test1=value2&test2=value2",
      method = Method.POST,
      headers = Headers(
        Seq(
          Header("TEST1", "value1, value2"),
          Header("TEST2", "value1")
        )
      ),
      cookies = Seq(
        CookieWithMeta.unsafeApply("test1", "value1"),
        CookieWithMeta.unsafeApply("test2", "value2")
      )
    )

    /**
     * A request pipeline which handles a request.
     */
    val requestPipeline = SilhouetteRequestPipeline(request)

    /**
     * A helper that creates a request pipeline with a body.
     *
     * @param body The body to create the request with.
     * @return A request pipeline with the given body.
     */
    def withBody(body: Body): SilhouetteRequestPipeline =
      SilhouetteRequestPipeline(request.copy(body = Some(body)))
  }
}
