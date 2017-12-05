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
package silhouette.http.client

import io.circe.Json
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import silhouette.exceptions.TransformException
import silhouette.http.client.DefaultBodyFormat._

import scala.xml.Node

/**
 * Test case for the [[DefaultBodyFormat]] class.
 */
class DefaultBodyFormatSpec extends Specification {

  "The infix 'as' function" should {
    "transform a Body as string" in new Context {
      validStringBody.as[String] must beSuccessfulTry.withValue(validString)
    }

    "transform a Body as json" in new Context {
      validJsonBody.as[Json] must beSuccessfulTry(parseJson(validJson))
    }

    "transform a Body as xml" in new Context {
      validXmlBody.as[Node] must beSuccessfulTry(parseXml(validXml))
    }

    "return a Failure on invalid json" in new Context {
      invalidJsonBody.as[Json] must beFailedTry.withThrowable[TransformException]
    }

    "return a Failure on invalid xml" in new Context {
      invalidJsonBody.as[Node] must beFailedTry.withThrowable[TransformException]
    }

    "return a Failure on unsupported content-type" in new Context {
      def errorMsg(expected: ContentType, actual: ContentType): String = UnsupportedContentType.format(expected, actual)
      validXmlBody.as[String] must beFailedTry.like {
        case e: UnsupportedContentTypeException =>
          e.getMessage must be equalTo errorMsg(ContentTypes.`text/plain`, ContentTypes.`application/xml`)
      }
      validXmlBody.as[Json] must beFailedTry.like {
        case e: UnsupportedContentTypeException =>
          e.getMessage must be equalTo errorMsg(ContentTypes.`application/json`, ContentTypes.`application/xml`)
      }
      validJsonBody.as[Node] must beFailedTry.like {
        case e: UnsupportedContentTypeException =>
          e.getMessage must be equalTo errorMsg(ContentTypes.`application/xml`, ContentTypes.`application/json`)
      }
    }
  }

  "The `write` method of the `BodyFormat[String]` format" should {
    "return a Body instance" in new Context {
      val body = BodyFormat.stringFormat.write(validString)

      body.contentType must be equalTo validStringBody.contentType
      body.codec must be equalTo validStringBody.codec
      body.data must be equalTo validStringBody.data
    }
  }

  "The `write` method of the `BodyFormat[Json]` format" should {
    "return a Body instance" in new Context {
      val body = BodyFormat.circeJsonFormat.write(parseJson(validJson))

      body.contentType must be equalTo validJsonBody.contentType
      body.codec must be equalTo validJsonBody.codec
      body.data must be equalTo validJsonBody.data
    }
  }

  "The `write` method of the `BodyFormat[Node]` format" should {
    "return a Body instance" in new Context {
      val body = BodyFormat.scalaXmlFormat.write(parseXml(validXml))

      body.contentType must be equalTo validXmlBody.contentType
      body.codec must be equalTo validXmlBody.codec
      body.data must be equalTo validXmlBody.data
    }
  }

  /**
   * The context.
   */
  trait Context extends Scope {
    val validString = "some string"
    val validJson = "{\"a\":\"b\"}"
    val invalidJson = "{a: }"
    val validXml = "<a>b</a>"
    val invalidXml = "<a>b<a>"

    val validStringBody = Body(
      contentType = ContentTypes.`text/plain`,
      data = validString.getBytes(Body.DefaultCodec.charSet)
    )
    val validJsonBody = Body(
      contentType = ContentTypes.`application/json`,
      data = validJson.getBytes(Body.DefaultCodec.charSet)
    )
    val invalidJsonBody = Body(
      contentType = ContentTypes.`application/json`,
      data = invalidJson.getBytes(Body.DefaultCodec.charSet)
    )
    val validXmlBody = Body(
      contentType = ContentTypes.`application/xml`,
      data = validXml.getBytes(Body.DefaultCodec.charSet)
    )
    val invalidXmlBody = Body(
      contentType = ContentTypes.`application/xml`,
      data = invalidXml.getBytes(Body.DefaultCodec.charSet)
    )

    /**
     * Helper method that parses JSON.
     *
     * @param str The JSON string to parse.
     * @return The Json object on success or an exception on failure.
     */
    def parseJson(str: String): Json = {
      io.circe.parser.parse(validJson) match {
        case Left(e)  => throw e
        case Right(j) => j
      }
    }

    /**
     * Helper method that parses XML.
     *
     * @param str The XML string to parse.
     * @return The XML Node object on success or an exception on failure.
     */
    def parseXml(str: String): Node = {
      scala.xml.XML.loadString(str)
    }
  }
}
