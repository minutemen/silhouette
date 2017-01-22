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
package silhouette.http.client.decoder

import java.nio.charset.StandardCharsets

import io.circe.Json
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import silhouette.exceptions.{ DecoderException, UnsupportedContentTypeException }
import silhouette.http.client.decoder.DefaultBodyDecoder._
import silhouette.http.client.{ Body, ContentType, ContentTypes }

import scala.xml.NodeSeq

/**
 * Test case for the [[DefaultBodyDecoder]] class.
 */
class DefaultBodyDecoderSpec extends Specification {

  "the infix 'as' function" should {
    "decode a Body as string" in new Context {
      validJsonBody.as[String] must beSuccessfulTry.withValue(validJson)
    }

    "decode a Body as json" in new Context {
      validJsonBody.as[Json] must beSuccessfulTry(io.circe.parser.parse(validJson).right.get)
    }

    "decode a Body as xml" in new Context {
      validXmlBody.as[NodeSeq] must beSuccessfulTry(scala.xml.XML.loadString(validXml))
    }

    "return a Failure on invalid json" in new Context {
      invalidJsonBody.as[Json] must beFailedTry.withThrowable[DecoderException]
    }

    "return a Failure on invalid xml" in new Context {
      invalidJsonBody.as[NodeSeq] must beFailedTry.withThrowable[DecoderException]
    }

    "return a Failure on unsupported cotent-type" in new Context {
      def errorMsg(expected: ContentType, actual: ContentType): String = UnsupportedContentType.format(expected, actual)
      validXmlBody.as[Json] must beFailedTry.like {
        case e: UnsupportedContentTypeException =>
          e.getMessage must be equalTo errorMsg(ContentTypes.`application/json`, ContentTypes.`application/xml`)
      }
      validJsonBody.as[NodeSeq] must beFailedTry.like {
        case e: UnsupportedContentTypeException =>
          e.getMessage must be equalTo errorMsg(ContentTypes.`application/xml`, ContentTypes.`application/json`)
      }

    }
  }

  /**
   * The context.
   */
  trait Context extends Scope {
    val validJson = "{\"a\": \"b\"}"
    val invalidJson = "{a: }"
    val validXml = "<a>b</a>"
    val invalidXml = "<a>b<a>"

    val validJsonBody = Body(
      contentType = ContentTypes.`application/json`,
      data = validJson.getBytes(StandardCharsets.UTF_8.toString)
    )
    val invalidJsonBody = Body(
      contentType = ContentTypes.`application/json`,
      data = invalidJson.getBytes(StandardCharsets.UTF_8.toString)
    )
    val validXmlBody = Body(
      contentType = ContentTypes.`application/xml`,
      data = validXml.getBytes(StandardCharsets.UTF_8.toString)
    )
    val invalidXmlBody = Body(
      contentType = ContentTypes.`application/xml`,
      data = invalidXml.getBytes(StandardCharsets.UTF_8.toString)
    )
  }
}
