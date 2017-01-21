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

import io.circe.parser._
import io.circe.{ Json, ParsingFailure }
import silhouette.exceptions.{ DecoderException, UnsupportedContentTypeException }
import silhouette.http.client.{ Body, ContentTypes }
import silhouette.http.client.decoder.DefaultBodyDecoder._

import scala.util.{ Failure, Try }
import scala.xml._

/**
 * Provides implicit decoders for the body types supported by Silhouette.
 */
trait DefaultBodyDecoder {

  /**
   * Encodes a [[Body]] into a Circe JSON object.
   *
   * @return A [[BodyDecoder]] instance that encodes a [[Body]] into a Circe JSON object.
   */
  implicit def circeJsonDecoder: BodyDecoder[Json] = new BodyDecoder[Json] {
    override def decode(in: Body): Try[Json] = in match {
      case Body(ContentTypes.`application/json`, codec, bytes) =>
        parse(new String(bytes, codec.charSet)).toTry.recover {
          case ParsingFailure(msg, e) => throw new DecoderException(msg, Option(e))
        }
      case Body(ct, _, _) =>
        Failure(new UnsupportedContentTypeException(UnsupportedContentType.format(ContentTypes.`application/json`, ct)))
    }
  }

  /**
   * Encodes a [[Body]] into a Scala XML object.
   *
   * @return A [[BodyDecoder]] instance that encodes a [[Body]] into a Scala XML object.
   */
  implicit def scalaXmlDecoder: BodyDecoder[NodeSeq] = new BodyDecoder[NodeSeq] {
    override def decode(in: Body): Try[NodeSeq] = in match {
      case Body(ContentTypes.`application/xml`, codec, bytes) =>
        Try(XML.loadString(new String(bytes, codec.charSet))).recover {
          case e: SAXParseException => throw new DecoderException(e.getMessage, Option(e))
        }
      case Body(ct, _, _) =>
        Failure(new UnsupportedContentTypeException(UnsupportedContentType.format(ContentTypes.`application/xml`, ct)))
    }
  }

  /**
   * Encodes a [[Body]] into string.
   *
   * @return A [[BodyDecoder]] instance that encodes a [[Body]] into a string.
   */
  implicit def stringDecoder: BodyDecoder[String] = new BodyDecoder[String] {
    override def decode(in: Body): Try[String] = in match {
      case Body(_, codec, bytes) => Try(new String(bytes, codec.charSet))
    }
  }
}

/**
 * The companion object.
 */
object DefaultBodyDecoder {

  /**
   * The error messages.
   */
  val UnsupportedContentType: String = "[Silhouette][DefaultBodyDecoder] Expected %s but found %s"
}
