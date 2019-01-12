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
import silhouette.TransformException
import silhouette.http.BodyFormat._
import silhouette.http.DefaultBodyFormat._
import silhouette.http.client.UnsupportedContentTypeException

import scala.xml.Node

/**
 * Test case for the [[silhouette.http.DefaultBodyFormat]] class.
 */
class DefaultBodyFormatSpec extends Specification {

  "The `raw` method of the Body" should {
    "return the raw string of a string body" in new Context {
      validStringBody.raw must be equalTo validString
    }

    "return the raw string of a form URL encoded body" in new Context {
      validFormUrlEncodedBody.raw must be equalTo validFormUrlEncodedString
    }

    "return the raw string of a JSON body" in new Context {
      validJsonBody.raw must be equalTo validJson
    }

    "return the raw string of a XML body" in new Context {
      validXmlBody.raw must be equalTo validXml
    }
  }

  "The infix 'as' method of the Body" should {
    "transform a Body as string" in new Context {
      validStringBody.as[String] must beSuccessfulTry.withValue(validString)
    }

    "transform a Body as Map[String, Seq[String]]" in new Context {
      validFormUrlEncodedBody.as[Map[String, Seq[String]]] must beSuccessfulTry(validFormUrlEncoded)
    }

    "transform a Body as Json object" in new Context {
      validJsonBody.as[Json] must beSuccessfulTry(parseJson(validJson))
    }

    "transform a Body as xml" in new Context {
      validXmlBody.as[Node] must beSuccessfulTry(parseXml(validXml))
    }

    "return a Failure on invalid json" in new Context {
      invalidJsonBody.as[Json].toEither must beLeft.like {
        case e =>
          e must beAnInstanceOf[TransformException]
      }
    }

    "return a Failure on invalid xml" in new Context {
      invalidJsonBody.as[Node].toEither must beLeft.like {
        case e =>
          e must beAnInstanceOf[TransformException]
      }
    }

    "return a Failure on unsupported content type" in new Context {
      validXmlBody.as[String] must beFailedTry.like {
        case e: UnsupportedContentTypeException =>
          e.getMessage must be equalTo errorMsg(MimeType.`text/plain`, XmlBody.contentType)
      }
      validXmlBody.as[Map[String, Seq[String]]] must beFailedTry.like {
        case e: UnsupportedContentTypeException =>
          e.getMessage must be equalTo errorMsg(
            MimeType.`application/x-www-form-urlencoded`, MimeType.`application/xml`
          )
      }
      validXmlBody.as[Json] must beFailedTry.like {
        case e: UnsupportedContentTypeException =>
          e.getMessage must be equalTo errorMsg(JsonBody.allowedTypes, MimeType.`application/xml`)
      }
      validJsonBody.as[Node] must beFailedTry.like {
        case e: UnsupportedContentTypeException =>
          e.getMessage must be equalTo errorMsg(XmlBody.allowedTypes, MimeType.`application/json`)
      }
    }
  }

  "The `read` method of the BodyFormat[String]` format" should {
    "throw an `UnsupportedContentTypeException` if the content type isn't of type `text/plain`" in new Context {
      BodyFormat.stringReads.read(validXmlBody) must beFailedTry.like {
        case e: UnsupportedContentTypeException =>
          e.getMessage must be equalTo errorMsg(MimeType.`text/plain`, MimeType.`application/xml`)
      }
    }

    "return a string from a body" in new Context {
      BodyFormat.stringReads.read(validStringBody) must beSuccessfulTry.withValue(validString)
    }
  }

  "The `write` method of the `BodyFormat[String]` format" should {
    "return a Body instance" in new Context {
      val body = BodyFormat.stringWrites(Body.DefaultCodec).write(validString)

      body.contentType must be equalTo validStringBody.contentType
      body.codec must be equalTo validStringBody.codec
      body.data must be equalTo validStringBody.data
    }
  }

  "The `read` method of the BodyFormat[Map[String, Seq[String]]]` format" should {
    "throw an `UnsupportedContentTypeException` if the content type isn't of type " +
      "`application/x-www-form-urlencoded`" in new Context {
        BodyFormat.formUrlEncodedReads.read(validXmlBody) must beFailedTry.like {
          case e: UnsupportedContentTypeException =>
            e.getMessage must be equalTo errorMsg(
              MimeType.`application/x-www-form-urlencoded`, MimeType.`application/xml`
            )
        }
      }

    "return a Map[String, Seq[String]] from a valid form URL encoded body" in new Context {
      BodyFormat.formUrlEncodedReads.read(validFormUrlEncodedBody) must beSuccessfulTry
        .withValue(validFormUrlEncoded)
    }

    "return a Map[String, Seq[String]] from an empty form URL encoded body" in new Context {
      BodyFormat.formUrlEncodedReads.read(emptyFormUrlEncodedBody) must beSuccessfulTry
        .withValue(emptyFormUrlEncoded)
    }
  }

  "The `write` method of the `BodyFormat[Map[String, Seq[String]]]` format" should {
    "return a Body instance" in new Context {
      val body = BodyFormat.formUrlEncodedWrites(Body.DefaultCodec).write(validFormUrlEncoded)

      body.contentType must be equalTo validFormUrlEncodedBody.contentType
      body.codec must be equalTo validFormUrlEncodedBody.codec
      body.data must be equalTo validFormUrlEncodedBody.data
    }
  }

  "The `read` method of the BodyFormat[Json]` format" should {
    "throw an `UnsupportedContentTypeException` if the content type isn't of type `application/json`" in new Context {
      BodyFormat.circeJsonReads.read(validXmlBody) must beFailedTry.like {
        case e: UnsupportedContentTypeException =>
          e.getMessage must be equalTo errorMsg(JsonBody.allowedTypes, MimeType.`application/xml`)
      }
    }

    "throw an `TransformException` if the given JSON body is invalid" in new Context {
      BodyFormat.circeJsonReads.read(invalidJsonBody) must beFailedTry.withThrowable[TransformException]
    }

    "return a Json object from a valid JSON body" in new Context {
      BodyFormat.circeJsonReads.read(validJsonBody) must beSuccessfulTry.withValue(parseJson(validJson))
    }

    "return a Json object from an empty JSON body" in new Context {
      BodyFormat.circeJsonReads.read(emptyJsonBody) must beSuccessfulTry.withValue(parseJson(emptyJson))
    }
  }

  "The `write` method of the `BodyFormat[Json]` format" should {
    "return a Body instance" in new Context {
      val body = BodyFormat.circeJsonWrites(Body.DefaultCodec).write(parseJson(validJson))

      body.contentType must be equalTo validJsonBody.contentType
      body.codec must be equalTo validJsonBody.codec
      body.data must be equalTo validJsonBody.data
    }
  }

  "The `read` method of the BodyFormat[Node]` format" should {
    "throw an `UnsupportedContentTypeException` if the content type isn't of type `application/xml`" in new Context {
      BodyFormat.scalaXmlReads.read(validJsonBody) must beFailedTry.like {
        case e: UnsupportedContentTypeException =>
          e.getMessage must be equalTo errorMsg(XmlBody.allowedTypes, MimeType.`application/json`)
      }
    }

    "throw an `TransformException` if the given XML body is invalid" in new Context {
      BodyFormat.scalaXmlReads.read(invalidXmlBody) must beFailedTry.withThrowable[TransformException]
    }

    "return a Node object from a valid XML body" in new Context {
      BodyFormat.scalaXmlReads.read(validXmlBody) must beSuccessfulTry.withValue(parseXml(validXml))
    }
  }

  "The `write` method of the `BodyFormat[Node]` format" should {
    "return a Body instance" in new Context {
      val body = BodyFormat.scalaXmlWrites(Body.DefaultCodec).write(parseXml(validXml))

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
    val validFormUrlEncoded = Map("param1" -> Seq("value 1"), "param2" -> Seq("value 2"))
    val emptyFormUrlEncoded = Map.empty[String, Seq[String]]
    val validFormUrlEncodedString = "param1=value+1&param2=value+2"
    val invalidFormUrlEncodedString = "param1=value 1&param2=value 2"
    val validJson = "{\"a\":\"b\"}"
    val invalidJson = "{a: }"
    val emptyJson = "{}"
    val validXml = "<a>b</a>"
    val invalidXml = "<a>b<a>"

    val validStringBody = Body.from(validString)
    val validFormUrlEncodedBody = Body.from(validFormUrlEncoded)
    val invalidFormUrlEncodedBody = Body(
      contentType = MimeType.`application/x-www-form-urlencoded`,
      data = invalidFormUrlEncodedString.getBytes(Body.DefaultCodec.charSet)
    )
    val emptyFormUrlEncodedBody = Body.from(emptyFormUrlEncoded)
    val validJsonBody = Body.from(parseJson(validJson))
    val invalidJsonBody = Body(
      contentType = MimeType.`application/json`,
      data = invalidJson.getBytes(Body.DefaultCodec.charSet)
    )
    val emptyJsonBody = Body.from(parseJson(emptyJson))
    val validXmlBody = Body.from(parseXml(validXml))
    val invalidXmlBody = Body(
      contentType = MimeType.`application/xml`,
      data = invalidXml.getBytes(Body.DefaultCodec.charSet)
    )

    /**
     * Helper message to create the `UnsupportedContentType` error messages.
     *
     * @param expected The expected mime type.
     * @param actual   The actual mime type.
     * @return The error message.
     */
    def errorMsg(expected: MimeType, actual: MimeType): String = UnsupportedContentType.format(expected, actual)

    /**
     * Helper message to create the `UnsupportedContentType` error messages.
     *
     * @param allowed The list of allowed mime types.
     * @param actual   The actual mime type.
     * @return The error message.
     */
    def errorMsg(allowed: Seq[MimeType], actual: MimeType): String =
      UnsupportedContentType.format(allowed.mkString(", "), actual)

    /**
     * Helper method that parses JSON.
     *
     * @param str The JSON string to parse.
     * @return The Json object on success or an exception on failure.
     */
    def parseJson(str: String): Json = {
      io.circe.parser.parse(str) match {
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
    def parseXml(str: String): Node = scala.xml.XML.loadString(str)
  }
}
